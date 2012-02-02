/*
 * Copyright (C) 2005-2011 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.portofino.dispatcher;

import com.manydesigns.portofino.application.Application;
import com.manydesigns.portofino.pages.Layout;
import com.manydesigns.portofino.pages.Page;
import com.manydesigns.portofino.util.PortofinoFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla - alessio.stalla@manydesigns.com
*/
public class PageInstance {

    protected final Application application;
    protected final Page page;
    protected final File directory;
    protected final List<String> parameters;
    protected final PageInstance parent;
    protected Object configuration;
    protected Class<? extends PageAction> actionClass;
    protected PageAction actionBean;
    protected String description;

    public static final String DETAIL = "_detail";

    //**************************************************************************
    // Logging
    //**************************************************************************

    public static final Logger logger = LoggerFactory.getLogger(PageInstance.class);

    public PageInstance(PageInstance parent, File directory, Application application, Page page) {
        this.parent = parent;
        this.directory = directory;
        this.application = application;
        this.page = page;
        this.parameters = new ArrayList<String>();
    }

    public Page getPage() {
        return page;
    }

    public Application getApplication() {
        return application;
    }

    //**************************************************************************
    // Utility Methods
    //**************************************************************************

    public String getUrlFragment() {
        String fragment = directory.getName();
        for(String param : parameters) {
            fragment += "/" + param;
        }
        return fragment;
    }

    public String getPath() {
        if(getParent() == null) {
            return "";
        } else {
            return getParent().getPath() + "/" + getUrlFragment();
        }
    }

    public File getDirectory() {
        return directory;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public Object getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Object configuration) {
        this.configuration = configuration;
    }

    public void setActionClass(Class<? extends PageAction> actionClass) {
        this.actionClass = actionClass;
    }

    public Class<? extends PageAction> getActionClass() {
        if(actionClass == null) {
            actionClass = DispatcherLogic.getActionClass(directory);
        }
        return actionClass;
    }

    public PageAction getActionBean() {
        return actionBean;
    }

    public void setActionBean(PageAction actionBean) {
        this.actionBean = actionBean;
    }

    public PageInstance getParent() {
        return parent;
    }

    public Layout getLayout() {
        if(getParameters().isEmpty()) {
            return getPage().getLayout();
        } else {
            return getPage().getDetailLayout();
        }
    }

    public void setLayout(Layout layout) {
        if(getParameters().isEmpty()) {
            getPage().setLayout(layout);
        } else {
            getPage().setDetailLayout(layout);
        }
    }

    public Page getChildPage(String name) throws Exception {
        File childDirectory = getChildPageDirectory(name);
        return DispatcherLogic.getPage(childDirectory);
    }

    public File getChildPageDirectory(String name) {
        File baseDir = getChildrenDirectory();
        return new File(baseDir, name);
    }

    public File getChildrenDirectory() {
        File baseDir = directory;
        if(!parameters.isEmpty()) {
            baseDir = new File(baseDir, DETAIL);
        }
        return baseDir;
    }

    public String getName() {
        return directory.getName();
    }

    public String getPathFromRoot() {
        return PortofinoFileUtils.getRelativePath(application.getPagesDir(), directory);
    }

    public String getDescription() {
        return description != null ? description : getName();
    }

    public void setDescription(String description) {
        this.description = description;
    }
}