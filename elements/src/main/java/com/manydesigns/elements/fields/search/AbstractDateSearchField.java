/*
 * Copyright (C) 2005-2016 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.manydesigns.elements.fields.search;

import com.manydesigns.elements.ElementsProperties;
import com.manydesigns.elements.annotations.DateFormat;
import com.manydesigns.elements.reflection.PropertyAccessor;
import com.manydesigns.elements.util.Util;
import com.manydesigns.elements.xml.XhtmlBuffer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public abstract class AbstractDateSearchField extends RangeSearchField {
    public static final String copyright =
            "Copyright (C) 2005-2016, ManyDesigns srl";

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected String datePattern;
    protected DateTimeFormatter dateTimeFormatter;
    protected boolean containsTime;
    protected static final Logger logger = LoggerFactory.getLogger(AbstractDateSearchField.class);

    //**************************************************************************
    // Constructors
    //**************************************************************************

    public AbstractDateSearchField(PropertyAccessor accessor) {
        this(accessor, null);
    }

    public AbstractDateSearchField(PropertyAccessor accessor, String prefix) {
        super(accessor, prefix);

        DateFormat dateFormatAnnotation =
                accessor.getAnnotation(DateFormat.class);
        if (dateFormatAnnotation != null) {
            datePattern = dateFormatAnnotation.value();
        } else {
            Configuration elementsConfiguration =
                    ElementsProperties.getConfiguration();
            datePattern = elementsConfiguration.getString(
                    ElementsProperties.FIELDS_DATE_FORMAT);
        }
        dateTimeFormatter = DateTimeFormat.forPattern(datePattern);
        setSize(dateTimeFormatter.getParser().estimateParsedLength());

        containsTime = datePattern.contains("HH")
                || datePattern.contains("mm")
                || datePattern.contains("ss");
    }

    //**************************************************************************
    // Element overrides
    //**************************************************************************

    @Override
    public void rangeEndToXhtml(XhtmlBuffer xb, String id,
                                String inputName, String stringValue, String label) {
        //Must be before to avoid breaking Bootstrap input-group
        String js = MessageFormat.format(
                "$(function() '{' setupDatePicker(''#{0}'', ''{1}''); '}');",
                StringEscapeUtils.escapeJavaScript(id),
                StringEscapeUtils.escapeJavaScript(datePattern));
        xb.writeJavaScript(js);
        super.rangeEndToXhtml(xb, id, inputName, stringValue, label);
    }

    @Override
    public void readFromRequest(HttpServletRequest req) {
        minStringValue = StringUtils.trimToNull(req.getParameter(minInputName));
        minValue = readDate(minStringValue);

        maxStringValue = StringUtils.trimToNull(req.getParameter(maxInputName));
        maxValue = readDate(maxStringValue);

        searchNullValue = (NULL_VALUE.equals(minStringValue)
                || NULL_VALUE.equals(maxStringValue));
    }

    protected Object readDate(String stringValue) {
        Object value;
        try {
            value = toDate(Util.parseDateTime(dateTimeFormatter, stringValue, containsTime));
        } catch (Exception e) {
            logger.debug("Value not parseable", e);
            try {
                value = toDate(new DateTime(Long.valueOf(stringValue)));
            } catch (Exception ex) {
                logger.debug("Value is not a date representation", ex);
                //We tried, we failed
                value = null;
            }
        }
        return value;
    }

    protected abstract Object toDate(DateTime dateTime);

    //**************************************************************************
    // Getters/setters
    //**************************************************************************


    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    public DateTimeFormatter getDateTimeFormatter() {
        return dateTimeFormatter;
    }

    public void setDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public boolean isContainsTime() {
        return containsTime;
    }

    public void setContainsTime(boolean containsTime) {
        this.containsTime = containsTime;
    }

}
