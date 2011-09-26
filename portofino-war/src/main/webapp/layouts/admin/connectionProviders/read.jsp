<%@ page contentType="text/html;charset=ISO-8859-1" language="java"
         pageEncoding="ISO-8859-1"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"
%><%@taglib prefix="mde" uri="/manydesigns-elements"
%><stripes:layout-render name="/skins/default/admin-page.jsp">
    <jsp:useBean id="actionBean" scope="request" type="com.manydesigns.portofino.actions.admin.ConnectionProvidersAction"/>
    <stripes:layout-component name="pageTitle">
        Connection provider: <c:out value="${actionBean.databaseName}"/>
    </stripes:layout-component>
    <stripes:layout-component name="contentHeader">
        <stripes:submit name="returnToList" value="<< Return to list" class="contentButton"/>
        <stripes:submit name="edit" value="Edit" class="contentButton"/>
        <stripes:submit name="test" value="Test" class="contentButton"/>
        <stripes:submit name="sync" value="Synchronize" class="contentButton"/>
        <stripes:submit name="delete" value="Delete"
        onclick="return confirm ('Are you sure?');"
        class="contentButton"/>
    </stripes:layout-component>
    <stripes:layout-component name="portletTitle">
        Connection provider: <c:out value="${actionBean.databaseName}"/>
    </stripes:layout-component>
    <stripes:layout-component name="portletBody">
        <mde:write name="actionBean" property="form"/>
        <c:if test="${actionBean.detectedValuesForm != null}">
            <div class="horizontalSeparator"></div>
            <h2>Detected values</h2>
            <mde:write name="actionBean" property="detectedValuesForm"/>
        </c:if>
        <stripes:hidden name="databaseName" value="${actionBean.databaseName}"/>
    </stripes:layout-component>
    <stripes:layout-component name="contentFooter">
        <stripes:submit name="returnToList" value="<< Return to list" class="contentButton"/>
        <stripes:submit name="edit" value="Edit" class="contentButton"/>
        <stripes:submit name="test" value="Test" class="contentButton"/>
        <stripes:submit name="sync" value="Synchronize" class="contentButton"/>
        <stripes:submit name="delete" value="Delete"
        onclick="return confirm ('Are you sure?');"
        class="contentButton"/>
    </stripes:layout-component>
</stripes:layout-render>