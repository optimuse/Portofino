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

package com.manydesigns.portofino.pageactions.changepassword;

import com.manydesigns.elements.annotations.Password;
import com.manydesigns.elements.annotations.Required;
import com.manydesigns.elements.forms.Form;
import com.manydesigns.elements.forms.FormBuilder;
import com.manydesigns.elements.messages.SessionMessages;
import com.manydesigns.elements.options.SelectionProvider;
import com.manydesigns.elements.reflection.PropertyAccessor;
import com.manydesigns.elements.text.OgnlSqlFormat;
import com.manydesigns.portofino.buttons.annotations.Button;
import com.manydesigns.portofino.buttons.annotations.Buttons;
import com.manydesigns.portofino.dispatcher.PageInstance;
import com.manydesigns.portofino.logic.SelectionProviderLogic;
import com.manydesigns.portofino.model.database.Database;
import com.manydesigns.portofino.model.database.Table;
import com.manydesigns.portofino.pageactions.AbstractPageAction;
import com.manydesigns.portofino.pageactions.PageActionName;
import com.manydesigns.portofino.pageactions.annotations.ConfigurationClass;
import com.manydesigns.portofino.pageactions.changepassword.configuration.ChangePasswordConfiguration;
import com.manydesigns.portofino.reflection.TableAccessor;
import com.manydesigns.portofino.security.AccessLevel;
import com.manydesigns.portofino.security.RequiresPermissions;
import net.sourceforge.stripes.action.*;
import org.hibernate.NonUniqueResultException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
@RequiresPermissions(level = AccessLevel.VIEW)
@PageActionName("Change Password")
@ConfigurationClass(ChangePasswordConfiguration.class)
public class ChangePasswordAction extends AbstractPageAction {
    public static final String copyright =
            "Copyright (c) 2005-2012, ManyDesigns srl";

    public static final Logger logger =
            LoggerFactory.getLogger(ChangePasswordAction.class);

    protected Form form;

    protected String oldPassword;
    protected String newPassword;

    //Conf
    protected ChangePasswordConfiguration configuration;
    protected Form configurationForm;

    @DefaultHandler
    public Resolution execute() {
        prepareForm();
        if(!isConfigurationValid()) {
            return forwardToPortletNotConfigured();
        }
        String fwd = "/layouts/changepassword/change.jsp";
        return new ForwardResolution(fwd);
    }

    protected boolean isConfigurationValid() {
        return configuration != null &&
               configuration.getActualDatabase() != null &&
               configuration.getActualTable() != null &&
               configuration.getProperty() != null;
    }

    protected void prepareForm() {
        form = new FormBuilder(getClass())
                    .configFields("oldPassword", "newPassword")
                    .build();
        form.readFromObject(this);
    }

    @Button(list = "changepassword", key = "commons.ok")
    public Resolution change() {
        prepareForm();
        if(!isConfigurationValid()) {
            return forwardToPortletNotConfigured();
        }
        form.readFromRequest(context.getRequest());
        if(form.validate()) {
            Object user = loadUser();
            if(user != null) {
                try {
                    PropertyAccessor pwdAccessor = getPasswordPropertyAccessor();
                    String oldPwd = getOldPasswordFromUser(user, pwdAccessor);
                    if(encrypt(oldPassword).equals(oldPwd)) {
                        savePassword(user, pwdAccessor);
                    } else {
                        SessionMessages.addErrorMessage(getMessage("changepasswordaction.wrong.password"));
                    }
                } catch (NoSuchFieldException e) {
                    logger.error("Password property accessor: no such field", e);
                    return forwardToPortletNotConfigured();
                }
            } else {
                return forwardToPortletNotConfigured();
            }
        }
        String fwd = "/layouts/changepassword/change.jsp";
        return new ForwardResolution(fwd);
    }

    @Override
    @Buttons({
        @Button(list = "configuration", key = "commons.cancel", order = 99),
        @Button(list = "changepassword",  key = "commons.cancel", order = 99)})
    public Resolution cancel() {
        return super.cancel();
    }

    //Implementation/hooks

    protected String getOldPasswordFromUser(Object user, PropertyAccessor pwdAccessor) {
        //TODO check type
        return (String) pwdAccessor.get(user);
    }

    protected Object loadUser() {
        Session session = application.getSession(configuration.getActualDatabase().getDatabaseName());
        OgnlSqlFormat sqlFormat = OgnlSqlFormat.create(configuration.getQuery());
        final String queryString = sqlFormat.getFormatString();
        final Object[] parameters = sqlFormat.evaluateOgnlExpressions(this);
        Query query = session.createQuery(queryString);
        for (int i = 0; i < parameters.length; i++) {
            query.setParameter(i, parameters[i]);
        }
        try {
            return query.uniqueResult();
        } catch (NonUniqueResultException e) {
            //TODO sessionMessages
            return null;
        }
    }

    protected PropertyAccessor getPasswordPropertyAccessor() throws NoSuchFieldException {
        Table table = configuration.getActualTable();
        TableAccessor accessor = new TableAccessor(table);
        return accessor.getProperty(configuration.getProperty());
    }

    protected void savePassword(Object user, PropertyAccessor pwdAccessor) {
        Session session = application.getSession(configuration.getActualDatabase().getDatabaseName());
        Table table = configuration.getActualTable();
        pwdAccessor.set(user, encrypt(newPassword));
        session.save(table.getActualEntityName(), user);
        session.getTransaction().commit();
    }

    protected String encrypt(String oldPassword) {
        return oldPassword;
    }


    @Button(list = "portletHeaderButtons", key = "commons.configure", order = 1, icon = "ui-icon-wrench")
    @RequiresPermissions(level = AccessLevel.DEVELOP)
    public Resolution configure() {
        prepareConfigurationForms();
        return new ForwardResolution("/layouts/changepassword/configure.jsp");
    }

    @Button(list = "configuration", key = "commons.updateConfiguration")
    @RequiresPermissions(level = AccessLevel.DEVELOP)
    public Resolution updateConfiguration() {
        prepareConfigurationForms();
        readPageConfigurationFromRequest();
        configurationForm.readFromRequest(context.getRequest());
        boolean valid = validatePageConfiguration();
        valid = configurationForm.validate() && valid;
        if(valid) {
            updatePageConfiguration();
            configurationForm.writeToObject(configuration);
            saveConfiguration(configuration);
            SessionMessages.addInfoMessage(getMessage("commons.configuration.updated"));
        }
        return cancel();
    }

    @Override
    protected void prepareConfigurationForms() {
        super.prepareConfigurationForms();
        FormBuilder formBuilder = new FormBuilder(ChangePasswordConfiguration.class);
        formBuilder.configFieldSetNames("Password change configuration");
        formBuilder.configFields("database", "query", "property");

        SelectionProvider databaseSelectionProvider =
                SelectionProviderLogic.createSelectionProvider(
                        "database",
                        model.getDatabases(),
                        Database.class,
                        null,
                        new String[] { "databaseName" });
        formBuilder.configSelectionProvider(databaseSelectionProvider, "database");

        configurationForm = formBuilder.build();
        configurationForm.readFromObject(configuration);
    }

    public Resolution prepare(PageInstance pageInstance, ActionBeanContext context) {
        this.pageInstance = pageInstance;
        this.configuration = (ChangePasswordConfiguration) pageInstance.getConfiguration();
        if(!pageInstance.getParameters().isEmpty()) {
            return new ErrorResolution(404);
        }
        return null;
    }

    public Form getForm() {
        return form;
    }

    @Password
    @Required
    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    @Password(confirmationRequired = true)
    @Required
    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public Form getConfigurationForm() {
        return configurationForm;
    }
}