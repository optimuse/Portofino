/*
 * Copyright (C) 2005-2012 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.manydesigns.portofino.dispatcher;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.manydesigns.elements.options.DefaultSelectionProvider;
import com.manydesigns.elements.options.SelectionProvider;
import com.manydesigns.elements.util.ElementsFileUtils;
import com.manydesigns.portofino.PortofinoProperties;
import com.manydesigns.portofino.application.Application;
import com.manydesigns.portofino.pages.Page;
import com.manydesigns.portofino.scripting.ScriptingUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public class DispatcherLogic {
    public static final String copyright =
            "Copyright (c) 2005-2012, ManyDesigns srl";

    public static final Logger logger = LoggerFactory.getLogger(DispatcherLogic.class);

    public static SelectionProvider createPagesSelectionProvider
            (Application application, File baseDir, File... excludes) {
        return createPagesSelectionProvider(application, baseDir, false, false, excludes);
    }

    public static SelectionProvider createPagesSelectionProvider
            (Application application, File baseDir, boolean includeRoot, boolean includeDetailChildren,
             File... excludes) {
        DefaultSelectionProvider selectionProvider = new DefaultSelectionProvider("pages");
        if (includeRoot) {
            Page rootPage;
            try {
                rootPage = getPage(baseDir);
            } catch (Exception e) {
                throw new RuntimeException("Couldn't load root page", e);
            }
            selectionProvider.appendRow("/", rootPage.getTitle() + " (top level)", true);
        }
        appendChildrenToPagesSelectionProvider
                (application, baseDir, baseDir, null, selectionProvider, includeDetailChildren, excludes);
        return selectionProvider;
    }

    protected static void appendChildrenToPagesSelectionProvider
            (Application application, File baseDir, File parentDir, String breadcrumb,
             DefaultSelectionProvider selectionProvider, boolean includeDetailChildren, File... excludes) {
        FileFilter filter = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        };
        for (File dir : parentDir.listFiles(filter)) {
            appendToPagesSelectionProvider
                    (application, baseDir, dir, breadcrumb, selectionProvider, includeDetailChildren, excludes);
        }
    }

    private static void appendToPagesSelectionProvider
            (Application application, File baseDir, File file, String breadcrumb,
             DefaultSelectionProvider selectionProvider, boolean includeDetailChildren, File... excludes) {
        if (ArrayUtils.contains(excludes, file)) {
            return;
        }
        if (PageInstance.DETAIL.equals(file.getName())) {
            if (includeDetailChildren) {
                breadcrumb += " (detail)"; //TODO I18n
                selectionProvider.appendRow
                        ("/" + ElementsFileUtils.getRelativePath(baseDir, file), breadcrumb, true);
                appendChildrenToPagesSelectionProvider
                        (application, baseDir, file, breadcrumb, selectionProvider, includeDetailChildren, excludes);
            }
        } else {
            Page page;
            try {
                page = getPage(file);
            } catch (Exception e) {
                throw new RuntimeException("Couldn't load page", e);
            }
            if (breadcrumb == null) {
                breadcrumb = page.getTitle();
            } else {
                breadcrumb = String.format("%s > %s", breadcrumb, page.getTitle());
            }
            selectionProvider.appendRow
                    ("/" + ElementsFileUtils.getRelativePath(baseDir, file), breadcrumb, true);
            appendChildrenToPagesSelectionProvider
                    (application, baseDir, file, breadcrumb, selectionProvider, includeDetailChildren, excludes);
        }
    }

    protected static final JAXBContext pagesJaxbContext;

    static {
        try {
            pagesJaxbContext = JAXBContext.newInstance(Page.class.getPackage().getName());
        } catch (JAXBException e) {
            throw new Error("Can't instantiate pages jaxb context", e);
        }
    }

    public static File savePage(PageInstance pageInstance) throws Exception {
        return savePage(pageInstance.getDirectory(), pageInstance.getPage());
    }

    public static File savePage(File directory, Page page) throws Exception {
        File pageFile = getPageFile(directory);
        Marshaller marshaller = pagesJaxbContext.createMarshaller();
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(page, pageFile);
        return pageFile;
    }

    protected static class FileCacheEntry<T> {
        public final T object;
        public final long lastModified;
        public final Application application;

        public FileCacheEntry(T object, long lastModified, Application application) {
            this.object = object;
            this.lastModified = lastModified;
            this.application = application;
        }
    }

    protected static final LoadingCache<File, FileCacheEntry<Page>> pageCache =
            CacheBuilder.newBuilder()
                    .maximumSize(1000) //TODO conf
                    .refreshAfterWrite(5, TimeUnit.SECONDS)
                    .build(new CacheLoader<File, FileCacheEntry<Page>>() {

                        protected Page doLoad(File key) throws Exception {
                            FileInputStream fileInputStream = new FileInputStream(key);
                            try {
                                Page page = loadPage(fileInputStream);
                                page.init();
                                return page;
                            } finally {
                                IOUtils.closeQuietly(fileInputStream);
                            }
                        }

                        @Override
                        public FileCacheEntry<Page> load(File key) throws Exception {
                            return new FileCacheEntry<Page>(doLoad(key), key.lastModified(), null);
                        }

                        @Override
                        public ListenableFuture<FileCacheEntry<Page>> reload(
                                final File key, FileCacheEntry<Page> oldValue)
                                throws Exception {
                            if (key.lastModified() > oldValue.lastModified) {
                                /*return ListenableFutureTask.create(new Callable<PageCacheEntry>() {
                                    public PageCacheEntry call() throws Exception {
                                        return doLoad(key);
                                    }
                                });*/
                                //TODO async?
                                return Futures.immediateFuture(
                                        new FileCacheEntry<Page>(doLoad(key), key.lastModified(), null));
                            } else {
                                return Futures.immediateFuture(oldValue);
                            }
                        }

                    });

    protected static final LoadingCache<File, FileCacheEntry<Object>> configurationCache =
            CacheBuilder.newBuilder()
                    .maximumSize(1000) //TODO conf
                    .refreshAfterWrite(5, TimeUnit.SECONDS)
                    .build(new CacheLoader<File, FileCacheEntry<Object>>() {

                        protected Object doLoad(File key) throws Exception {
                            FileInputStream fileInputStream = new FileInputStream(key);
                            try {
                                Page page = loadPage(fileInputStream);
                                page.init();
                                return page;
                            } finally {
                                IOUtils.closeQuietly(fileInputStream);
                            }
                        }

                        @Override
                        public FileCacheEntry<Object> load(File key) throws Exception {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public ListenableFuture<FileCacheEntry<Object>> reload(
                                final File key, FileCacheEntry<Object> oldValue)
                                throws Exception {
                            if (key.lastModified() > oldValue.lastModified) {
                                /*return ListenableFutureTask.create(new Callable<PageCacheEntry>() {
                                    public PageCacheEntry call() throws Exception {
                                        return doLoad(key);
                                    }
                                });*/
                                //TODO async?
                                Object newConf = loadConfiguration(
                                        key, oldValue.application, oldValue.object.getClass());
                                return Futures.immediateFuture(
                                        new FileCacheEntry<Object>(newConf, key.lastModified(), null));
                            } else {
                                return Futures.immediateFuture(oldValue);
                            }
                        }

                    });

    protected static File getPageFile(File directory) {
        return new File(directory, "page.xml");
    }

    public static Page loadPage(InputStream inputStream) throws JAXBException {
        Unmarshaller unmarshaller = pagesJaxbContext.createUnmarshaller();
        return (Page) unmarshaller.unmarshal(inputStream);
    }

    public static Page getPage(File directory) throws Exception {
        return pageCache.get(getPageFile(directory)).object;
    }

    public static File saveConfiguration(File directory, Object configuration) throws Exception {
        String configurationPackage = configuration.getClass().getPackage().getName();
        JAXBContext jaxbContext = JAXBContext.newInstance(configurationPackage);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        File configurationFile = new File(directory, "configuration.xml");
        marshaller.marshal(configuration, configurationFile);
        return configurationFile;
    }

    public static <T> T getConfiguration(
            File configurationFile, Application application, Class<? extends T> configurationClass)
            throws Exception {
        FileCacheEntry<Object> entry = configurationCache.getIfPresent(configurationFile);
        if(entry == null) {
            T conf = loadConfiguration(configurationFile, application, configurationClass);
            entry = new FileCacheEntry<Object>(conf, configurationFile.lastModified(), application);
            configurationCache.put(configurationFile, entry);
        }
        return (T) entry.object;
    }

    protected static <T> T loadConfiguration(
            File configurationFile, Application application, Class<? extends T> configurationClass) throws Exception {
        if (configurationClass == null) {
            return null;
        }
        InputStream inputStream = new FileInputStream(configurationFile);
        try {
            return loadConfiguration(inputStream, application, configurationClass);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    protected static <T> T loadConfiguration
            (InputStream inputStream, Application application, Class<? extends T> configurationClass) throws Exception {
        if (configurationClass == null) {
            return null;
        }
        Object configuration;
        String configurationPackage = configurationClass.getPackage().getName();
        JAXBContext jaxbContext = JAXBContext.newInstance(configurationPackage);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        configuration = unmarshaller.unmarshal(inputStream);
        if (!configurationClass.isInstance(configuration)) {
            logger.error("Invalid configuration: expected " + configurationClass + ", got " + configuration);
            return null;
        }
        if(configuration instanceof PageActionConfiguration) {
            ((PageActionConfiguration) configuration).init(application);
        }
        return (T) configuration;
    }

    public static Class<? extends PageAction> getActionClass(Application application, File directory) {
        return getActionClass(application, directory, true);
    }

    public static Class<? extends PageAction> getActionClass
            (Application application, File directory, boolean fallback) {
        File scriptFile = ScriptingUtil.getGroovyScriptFile(directory, "action");
        Class<? extends PageAction> actionClass;
        try {
            actionClass = (Class<? extends PageAction>) ScriptingUtil.getGroovyClass(scriptFile);
        } catch (Exception e) {
            logger.error("Couldn't load action class for " + directory + ", returning safe-mode action", e);
            return fallback ? getFallbackActionClass(application) : null;
        }
        if (isValidActionClass(actionClass)) {
            return actionClass;
        } else {
            logger.error("Invalid action class for " + directory + ": " + actionClass);
            return fallback ? getFallbackActionClass(application) : null;
        }
    }

    protected static Class<? extends PageAction> getFallbackActionClass(Application application) {
        Configuration configuration = application.getPortofinoProperties();
        String className = configuration.getString(PortofinoProperties.FALLBACK_ACTION_CLASS);
        try {
            Class<?> aClass = Class.forName(className);
            if (isValidActionClass(aClass)) {
                return (Class<? extends PageAction>) aClass;
            } else {
                throw new Error("Configuration error, invalid fallback action class: " + className);
            }
        } catch (Throwable e) {
            throw new Error("Configuration error, fallback action class not found: " + className, e);
        }
    }

    public static boolean isValidActionClass(Class<?> actionClass) {
        if (actionClass == null) {
            return false;
        }
        if (!PageAction.class.isAssignableFrom(actionClass)) {
            logger.error("Action " + actionClass + " must implement " + PageAction.class);
            return false;
        }
        return true;
    }
}
