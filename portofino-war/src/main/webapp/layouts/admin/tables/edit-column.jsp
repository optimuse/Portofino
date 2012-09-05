<%@ page contentType="text/html;charset=UTF-8" language="java"
         pageEncoding="UTF-8"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"
%><%@ taglib prefix="mde" uri="/manydesigns-elements"
%><%@ taglib tagdir="/WEB-INF/tags" prefix="portofino"
%><%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<stripes:layout-render name="/skins/default/admin-page.jsp">
    <jsp:useBean id="actionBean" scope="request" type="com.manydesigns.portofino.actions.admin.TablesAction"/>
    <stripes:layout-component name="pageTitle">
        <fmt:message key="layouts.admin.tables.editColumn.title">
            <fmt:param value="${actionBean.column.qualifiedName}" />
        </fmt:message>
    </stripes:layout-component>
    <stripes:layout-component name="contentHeader">
        <portofino:buttons list="column-edit" cssClass="contentButton" />
    </stripes:layout-component>
    <stripes:layout-component name="portletTitle">
        <fmt:message key="layouts.admin.tables.editColumn.title">
            <fmt:param value="${actionBean.column.qualifiedName}" />
        </fmt:message>
    </stripes:layout-component>
    <stripes:layout-component name="portletBody">
        <mde:write name="actionBean" property="columnForm" />
        <mde:write name="actionBean" property="tableForm" />
    </stripes:layout-component>
    <stripes:layout-component name="contentFooter">
        <portofino:buttons list="column-edit" cssClass="contentButton" />
    </stripes:layout-component>
</stripes:layout-render>