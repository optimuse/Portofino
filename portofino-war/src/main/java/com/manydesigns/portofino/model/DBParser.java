/*
 * Copyright (C) 2005-2010 ManyDesigns srl.  All rights reserved.
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
package com.manydesigns.portofino.model;

import org.apache.commons.lang.StringUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Angelo    Lupo       - angelo.lupo@manydesigns.com
 * @author Paolo     Predonzani - paolo.predonzani@manydesigns.com
 */
public class DBParser {
    private static final String DATAMODEL = "datamodel";
    private static final String DATABASE = "database";
    private static final String CONNECTION = "connection";
    private static final String SCHEMA = "schema";
    private static final String SCHEMAS = "schemas";
    private static final String TABLE = "table";
    private static final String TABLES = "tables";
    private static final String COLUMN = "column";
    private static final String COLUMNS = "columns";
    private static final String PRIMARY_KEY = "primaryKey";
    private static final String RELATIONSHIP = "relationship";
    private static final String RELATIONSHIPS = "relationships";
    private static final String REFERENCE = "reference";
    private List<RelationshipPre> relationships;
    protected ClassLoader classLoader;

    public DBParser() {
        classLoader = this.getClass().getClassLoader();
    }

    public DataModel parse(String fileName) throws Exception {
        DataModel dataModel = new DataModel();
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        ClassLoader cl = this.getClass().getClassLoader();
        InputStream input = cl.getResourceAsStream(fileName);
        XMLStreamReader xmlStreamReader = inputFactory.createXMLStreamReader(input);
        doStartDocument(xmlStreamReader, dataModel);
        return dataModel;
    }

    private void doStartDocument(XMLStreamReader xmlStreamReader,
                                 DataModel dataModel) throws Exception {
        relationships = new ArrayList<RelationshipPre>();
        int event = -1;
        while (xmlStreamReader.hasNext()) {
            if (event == XMLStreamConstants.END_ELEMENT
                    && xmlStreamReader.getLocalName().equals(DATAMODEL)) {
                break;
            }
            event = next(xmlStreamReader);

            if (event == XMLStreamConstants.START_ELEMENT
                    && xmlStreamReader.getLocalName().equals(DATAMODEL)) {
                doDataModel(xmlStreamReader, dataModel);
            }
        }
    }

    private void doDataModel(XMLStreamReader xmlStreamReader, DataModel dataModel)
            throws Exception {
         doDataBase(xmlStreamReader, dataModel);
    }

    private void doDataBase(XMLStreamReader xmlStreamReader, DataModel dataModel)
            throws Exception {
        int event=-1;
        String lName=null;
        int dbopen = 0;
        boolean dbpresent = false;


        while (xmlStreamReader.hasNext()) {
            if (event == XMLStreamConstants.END_ELEMENT && DATABASE.equals(lName)) {
                break;
            }
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.START_ELEMENT && lName.equals(DATABASE)) {
                dbpresent = true;
                dbopen++;

                String attName;
                String attValue;
                String name;

                attName = xmlStreamReader.getAttributeLocalName(0);
                attValue = xmlStreamReader.getAttributeValue(0);

                if (attName.equals("name")) {
                   name = attValue;
                } else {
                    throw new Exception("TAG " + DATABASE +
                            ", attr name non presente");
                }

                Connection conn = doConnection(xmlStreamReader);
                Database db = new Database(name, conn);
                dataModel.getDatabases().add(db);
                doSchemas(xmlStreamReader, dataModel, db);
            }

            if (event == XMLStreamConstants.END_ELEMENT && lName.equals(DATABASE)) {
                dbopen--;
            }
        }
        if (!dbpresent) {
            throw new Exception("TAG " + DATABASE + " non presente");
        }
        if (dbopen != 0) {
            throw new Exception("TAG " + DATABASE + " non chiuso");
        }

        createRelationshipsPost(dataModel);
    }

    private Connection doConnection(XMLStreamReader xmlStreamReader)
            throws Exception {
        Connection conn = new Connection();
        int event = next(xmlStreamReader);
        String lName = xmlStreamReader.getLocalName();
        if (event == XMLStreamConstants.START_ELEMENT && lName.equals(CONNECTION)) {
            String attName;
            String attValue;
            List<String> expectedValList = new ArrayList<String>();
            expectedValList.add("type");
            expectedValList.add("driver");
            expectedValList.add("url");
            expectedValList.add("username");
            expectedValList.add("password");
            for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
                attName = xmlStreamReader.getAttributeLocalName(i);
                attValue = xmlStreamReader.getAttributeValue(i);

                if (attName.equals("type")) {
                    expectedValList.remove(attName);
                    conn.setType(attValue);
                    continue;
                }
                if (attName.equals("driver")) {
                    expectedValList.remove(attName);
                    conn.setDriverClass(attValue);
                    continue;
                }
                if (attName.equals("url")) {
                    expectedValList.remove(attName);
                    conn.setConnectionUrl(attValue);
                    continue;
                }
                if (attName.equals("username")) {
                    expectedValList.remove(attName);
                    conn.setUsername(attValue);
                    continue;
                }
                if (attName.equals("password")) {
                    expectedValList.remove(attName);
                    conn.setPassword(attValue);

                }
            }

            if (expectedValList.size() != 0) {

                throw new Exception(MessageFormat.format("Non sono presenti gli " +
                        "attributi {0} di Connection",
                        StringUtils.join(expectedValList, ", ")));
            }


            if (xmlStreamReader.hasNext()) {
                event = next(xmlStreamReader);
                lName = xmlStreamReader.getLocalName();
                if (event != XMLStreamConstants.END_ELEMENT
                        || !lName.equals(CONNECTION)) {
                    throw new Exception(
                            MessageFormat.format("TAG {0} non chiuso", CONNECTION));
                }
            }
        } else {
            throw new Exception("TAG Connection non presente");
        }
        return conn;
    }

    private void doSchemas(XMLStreamReader xmlStreamReader,
                           DataModel dm, Database db) throws Exception {
        int event=-1;
        String lName=null;
        int schemaOpen = 0;
        boolean schemaPresent = false;
        if (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.START_ELEMENT && lName.equals(SCHEMAS)) {
            } else {
                throw new Exception(
                        MessageFormat.format("TAG {0}s non presente", SCHEMA));
            }
        }
        while (xmlStreamReader.hasNext()) {
            if (event == XMLStreamConstants.END_ELEMENT && SCHEMA.equals(lName)) {
                schemaOpen--;
                break;
            }
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.START_ELEMENT && lName.equals(SCHEMA)) {
                schemaPresent = true;
                schemaOpen++;
                Schema schema;

                String attName = xmlStreamReader.getAttributeLocalName(0);
                String attValue = xmlStreamReader.getAttributeValue(0);

                if (attName.equals("name")) {
                    schema = new Schema(db.getDatabaseName(),attValue);
                } else {
                    throw new Exception(
                            MessageFormat.format("TAG {0}, ATTR Name non presente",
                                    SCHEMA));
                }
                db.getSchemas().add(schema);
                doTables(xmlStreamReader, dm, schema);
            }


        }
        if (!schemaPresent) {
            throw new Exception("TAG " + SCHEMA + " non presente");
        }
        if (schemaOpen != 0) {
            throw new Exception("TAG " + SCHEMA + " non chiuso");
        }

        if (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.END_ELEMENT && lName.equals(SCHEMAS)) {
            } else {
                throw new Exception(
                        MessageFormat.format("TAG {0}s non chiuso", SCHEMA));
            }
        }
    }

    private void doTables(XMLStreamReader xmlStreamReader, DataModel dm, Schema schema) throws Exception {
        int event;
        String lName;
        int tableOpen = 0;
        boolean tablePresent = false;
        if (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.START_ELEMENT && lName.equals(TABLES)) {
            } else {
                throw new Exception("TAG " + TABLES + " non presente");
            }
        }
        while (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.END_ELEMENT && lName.equals(TABLES)) {

                return;
            }
            if (event == XMLStreamConstants.END_ELEMENT && lName.equals(TABLE)) {
                tableOpen--;
                next(xmlStreamReader);
                xmlStreamReader.getLocalName();
                break;
            }
            if (event == XMLStreamConstants.START_ELEMENT && lName.equals(TABLE)) {
                tablePresent = true;
                tableOpen++;

                String attName = xmlStreamReader.getAttributeLocalName(0);
                String attValue = xmlStreamReader.getAttributeValue(0);
                String tableName;
                if (attName.equals("name")) {
                    tableName = attValue;
                } else {
                    throw new Exception(
                            MessageFormat.format("TAG {0}, ATTR Name non presente",
                                    SCHEMA));
                }
                Table table = new Table(schema.getDatabaseName(), schema.getSchemaName(),
                        tableName);
                schema.getTables().add(table);
                doColumns(xmlStreamReader, table);
                doPrimaryKey(xmlStreamReader, table);
                doRelationship(xmlStreamReader, dm, table);
            }
        }
    }

    private void doColumns(XMLStreamReader xmlStreamReader, Table table) throws Exception {
        int event;
        String lName;
        int columnOpen = 0;

        if (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.START_ELEMENT && lName.equals(COLUMNS)) {
            } else {
                throw new Exception("TAG " + COLUMNS + " non presente");
            }
        }
        while (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.END_ELEMENT && lName.equals(COLUMNS)) {
                return;
            }
            if (event == XMLStreamConstants.START_ELEMENT && lName.equals(COLUMN)) {
                columnOpen++;

                List<String> expectedValList = new ArrayList<String>();
                String name = null;
                String type = null;
                String javatype = null;
                int length = 0;
                boolean nullable = false;
                int scale=0;

                expectedValList.add("name");
                expectedValList.add("columnType");
                expectedValList.add("javaType");
                expectedValList.add("length");
                expectedValList.add("nullable");
                expectedValList.add("scale");

                for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
                    String attName = xmlStreamReader.getAttributeLocalName(i);
                    String attValue = xmlStreamReader.getAttributeValue(i);

                    if (attName.equals("name")) {
                        name = attValue;
                        expectedValList.remove(attName);
                        continue;
                    }
                    if (attName.equals("columnType")) {
                        type=attValue;
                        expectedValList.remove(attName);
                        continue;
                    }
                    if (attName.equals("javaType")) {
                        javatype = attValue;
                        expectedValList.remove(attName);
                        continue;
                    }
                    if (attName.equals("length")) {
                        length = Integer.parseInt(attValue);
                        continue;
                    }
                    if (attName.equals("nullable")) {
                        nullable = Boolean.parseBoolean(attValue);
                        continue;
                    }
                   
                    if (attName.equals("scale")) {
                        scale = Integer.parseInt(attValue);
                    }
                }

                if (expectedValList.size() != 0) {

                    throw new Exception(MessageFormat.format("Non sono presenti gli " +
                            "attributi {0} di Column", StringUtils.join(expectedValList, ", ")));
                }
                Column col = new Column(table.getDatabaseName(), table.getSchemaName(),
                  table.getTableName(), name,
                  type, nullable, length,
                  scale);
                table.getColumns().add(col);


            }

            if (event == XMLStreamConstants.END_ELEMENT && lName.equals(COLUMN)) {
                columnOpen--;
            }
        }

        if (columnOpen != 0) {
            throw new Exception("TAG " + COLUMN + " non chiuso");
        }

        if (xmlStreamReader.hasNext()) {
            event = xmlStreamReader.next();
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.END_ELEMENT && lName.equals(SCHEMAS)) {
            } else {
                throw new Exception(
                        MessageFormat.format("TAG {0} non chiuso", COLUMNS));
            }
        }
    }

    private void doPrimaryKey(XMLStreamReader xmlStreamReader, Table table) throws Exception {
        PrimaryKey pk;
        int event = next(xmlStreamReader);
        String lName = xmlStreamReader.getLocalName();
        if (event == XMLStreamConstants.START_ELEMENT && lName.equals(PRIMARY_KEY)) {
            String attName;
            String attValue;
            attName = xmlStreamReader.getAttributeLocalName(0);
            attValue = xmlStreamReader.getAttributeValue(0);
            if (attName.equals("name")) {
                pk = new PrimaryKey(
                        table.getDatabaseName(),
                        table.getSchemaName(),
                        table.getTableName(),
                        attValue);
            } else {
                throw new Exception(MessageFormat.format(
                        "TAG Primary Key, ATTR {0} non presente", PRIMARY_KEY));
            }
            do {
                event = next(xmlStreamReader);
                xmlStreamReader.getLocalName();
                attValue = xmlStreamReader.getAttributeValue(0);
                pk.getColumns().add(getColumn(table, attValue));
                event = next(xmlStreamReader);
                lName = xmlStreamReader.getLocalName();
                if (event != XMLStreamConstants.END_ELEMENT
                        && !lName.equals(COLUMN)) {
                    throw new Exception("TAG "
                            + COLUMN + " in Primary Key non chiuso");
                }

            }
            while (lName.equals(COLUMN) && event
                    == XMLStreamConstants.START_ELEMENT);
            table.setPrimaryKey(pk);
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event != XMLStreamConstants.END_ELEMENT
                    && !lName.equals(PRIMARY_KEY)) {
                throw new Exception("TAG Primary Key non chiuso");
            }
        }
    }

    private void doRelationship(XMLStreamReader xmlStreamReader, DataModel dm,
                                Table table) throws Exception {
        int event;
        String lName;
        if (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.START_ELEMENT &&
                    lName.equals(RELATIONSHIPS)) {
            } else {
                return;
            }
        }

        RelationshipPre rel = new RelationshipPre();
        if (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.START_ELEMENT &&
                    lName.equals(RELATIONSHIP)) {
            } else {
                throw new Exception("Tag " + RELATIONSHIP + " missing");
            }
        }

        for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
            String attName = xmlStreamReader.getAttributeLocalName(i);
            String attValue = xmlStreamReader.getAttributeValue(i);

            if (attName.equals("name")) {
                rel.setRelationshipName(attValue);
                continue;
            }
            if (attName.equals("toSchema")) {
                rel.setToSchema(attValue);
                continue;
            }
            if (attName.equals("toTable")) {

                rel.setToTable(attValue);
                continue;
            }
            if (attName.equals("onUpdate")) {
                rel.setOnUpdate(attValue);
                continue;
            }
            if (attName.equals("onDelete")) {
                rel.setOnDelete(attValue);

            }

        }


        // from (many) side
        rel.setFromTable(table.getTableName());
        rel.setFromSchema(table.getSchemaName());

        if (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.START_ELEMENT &&
                    lName.equals(REFERENCE)) {
                String fromCol = null, toCol = null;
                for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {
                    String attName = xmlStreamReader.getAttributeLocalName(i);
                    String attValue = xmlStreamReader.getAttributeValue(i);
                    if (attName.equals("fromColumn")) {
                        fromCol = attValue;
                        continue;
                    }
                    if (attName.equals("toColumn")) {
                        toCol = attValue;

                    }
                }

                ReferencePre ref = new ReferencePre();
                ref.setFromColumn(fromCol);
                ref.setToColumn(toCol);
                rel.getReferences().add(ref);
                if (xmlStreamReader.hasNext()) {
                    event = next(xmlStreamReader);
                    lName = xmlStreamReader.getLocalName();
                    if (event == XMLStreamConstants.END_ELEMENT
                            && lName.equals(REFERENCE)) {
                    } else {
                        throw new Exception("TAG Reference non chiuso");
                    }
                }
            }
        }

        relationships.add(rel);

        if (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();
            if (event == XMLStreamConstants.END_ELEMENT
                    && lName.equals(RELATIONSHIP)) {

            } else {
                throw new Exception("TAG Relationship non chiuso");
            }
        }

        if (xmlStreamReader.hasNext()) {
            event = next(xmlStreamReader);
            lName = xmlStreamReader.getLocalName();

            if (event == XMLStreamConstants.END_ELEMENT
                    && lName.equals(RELATIONSHIPS)) {

            } else {
                throw new Exception("TAG Relationships non chiuso");
            }
        }

    }

    private void createRelationshipsPost(DataModel dm) throws Exception {
        for (RelationshipPre relPre: relationships) {
            Relationship rel = new Relationship(relPre.getRelationshipName(),
                    relPre.getOnUpdate(), relPre.getOnDelete());
            final Table fromTable = getTable(dm, relPre.getFromSchema(), relPre.getFromTable());
            final Table toTable = getTable(dm, relPre.getToSchema(), relPre.getToTable());
            rel.setFromTable(fromTable);
            rel.setToTable(toTable);
            fromTable.getManyToOneRelationships().add(rel);
            toTable.getOneToManyRelationships().add(rel);

            for (ReferencePre refPre: relPre.getReferences()) {
                Reference ref = new Reference(getColumn(fromTable, refPre.getFromColumn()),
                        getColumn(toTable, refPre.getToColumn()));
                rel.getReferences().add(ref);
            }
        }
    }

    private Table getTable(DataModel dm, String schemaName, String tableName)
            throws Exception {
        for (Database db : dm.getDatabases()) {
            for (Schema schema : db.getSchemas()) {
                if (schemaName.equals(schema.getSchemaName())){
                    for (Table tb : schema.getTables()) {
                        if(tableName.equals(tb.getTableName()))
                            return tb;
                    }
                }
            }

        }
        throw new Exception("Tabella non presente");

    }

    private Column getColumn(Table table, String attValue) throws Exception {
        for (Column col : table.getColumns()) {
            if (col.getColumnName().equals(attValue)) {
                return col;
            }
        }
        throw new Exception("Colonna non presente");
    }

    private int next(XMLStreamReader xmlStreamReader) throws Exception {
        if (xmlStreamReader.hasNext()) {
            int event = xmlStreamReader.next();
            if (event == 1 || event == 2) {
                return event;
            } else {
                return next(xmlStreamReader);
            }
        } else {
            throw new Exception("Fine inattesa");
        }
    }


    public static void main(String[] argv) {

        try {
            new DBParser().parse("jpetstore-postgres.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

class RelationshipPre {
    public String fromDB;
    public String toDB;
    public String fromSchema;
    public String toSchema;
    public String fromTable;
    public String toTable;
    public String relationshipName;
    public String onUpdate;
    public String onDelete;
    List<ReferencePre>references = new ArrayList<ReferencePre>();

    public String getFromTable() {
        return fromTable;
    }

    public void setFromTable(String fromTable) {
        this.fromTable = fromTable;
    }

    public String getToTable() {
        return toTable;
    }

    public void setToTable(String toTable) {
        this.toTable = toTable;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public String getOnUpdate() {
        return onUpdate;
    }

    public void setOnUpdate(String onUpdate) {
        this.onUpdate = onUpdate;
    }

    public String getOnDelete() {
        return onDelete;
    }

    public void setOnDelete(String onDelete) {
        this.onDelete = onDelete;
    }

    public List<ReferencePre> getReferences() {
        return references;
    }

    public void setReferences(List<ReferencePre> references) {
        this.references = references;
    }

    public String getFromDB() {
        return fromDB;
    }

    public void setFromDB(String fromDB) {
        this.fromDB = fromDB;
    }

    public String getToDB() {
        return toDB;
    }

    public void setToDB(String toDB) {
        this.toDB = toDB;
    }

    public String getFromSchema() {
        return fromSchema;
    }

    public void setFromSchema(String fromSchema) {
        this.fromSchema = fromSchema;
    }

    public String getToSchema() {
        return toSchema;
    }

    public void setToSchema(String toSchema) {
        this.toSchema = toSchema;
    }
}

class ReferencePre {
    public String fromColumn;
    public String toColumn;


    public String getFromColumn() {
        return fromColumn;
    }

    public void setFromColumn(String fromColumn) {
        this.fromColumn = fromColumn;
    }

    public String getToColumn() {
        return toColumn;
    }

    public void setToColumn(String toColumn) {
        this.toColumn = toColumn;
    }
}