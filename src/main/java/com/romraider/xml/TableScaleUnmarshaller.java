/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2021 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.xml;

import static com.romraider.xml.DOMHelper.unmarshallAttribute;
import static com.romraider.xml.DOMHelper.unmarshallText;
import static org.w3c.dom.Node.ELEMENT_NODE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.modelmbean.XMLParseException;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.romraider.Settings;
import com.romraider.editor.ecu.ECUEditorManager;
import com.romraider.maps.DataCell;
import com.romraider.maps.Rom;
import com.romraider.maps.Scale;
import com.romraider.maps.Table;
import com.romraider.maps.Table1D;
import com.romraider.maps.Table2D;
import com.romraider.maps.Table3D;
import com.romraider.maps.TableBitwiseSwitch;
import com.romraider.maps.TableSwitch;
import com.romraider.swing.DebugPanel;
import com.romraider.util.ObjectCloner;
import com.romraider.util.SettingsManager;

public class TableScaleUnmarshaller {
    private static final Logger LOGGER = Logger.getLogger(TableScaleUnmarshaller.class);
	private final Map<String, Integer> tableNames = new HashMap<String, Integer>();
    private final List<Scale> scales = new ArrayList<Scale>();
    private String memModelEndian = null;
    private final Scale rawScale = new Scale();
    
    public void setMemModelEndian(String endian) {
    	memModelEndian = endian;
    }
    
    public void unmarshallBaseScales(Node rootNode) {
    	NodeList nodes = rootNode.getChildNodes();
        Node n;

        for (int i = 0; i < nodes.getLength(); i++) {
            n = nodes.item(i);

            if (n.getNodeType() == ELEMENT_NODE
                    && n.getNodeName().equalsIgnoreCase("scalingbase")) {
                scales.add(unmarshallScale(n, new Scale()));
            }
        }
    }
    
	 public Table unmarshallTable(Node tableNode, Table table, Rom rom)
	            throws XMLParseException, TableIsOmittedException, Exception {
	    			 
	        if (unmarshallAttribute(tableNode, "omit", "false").equalsIgnoreCase(
	                "true")) { // remove table if omitted
	            throw new TableIsOmittedException();
	        }

	        if (!unmarshallAttribute(tableNode, "base", "none").equalsIgnoreCase(
	                "none")) { // copy base table for inheritance
	            try {
	            	//Why is this needed?
	                table = (Table) ObjectCloner
	                        .deepCopy(rom.getTableByName(unmarshallAttribute(tableNode,
	                                "base", "none")));

	            } catch (TableNotFoundException ex) { /* table not found, do nothing */

	            } catch (InvalidTableNameException ex) { // Table name is invalid, do nothing.

	            } catch (NullPointerException ex) {
	                JOptionPane.showMessageDialog(ECUEditorManager.getECUEditor(),
	                        new DebugPanel(ex, SettingsManager.getSettings().getSupportURL()), "Exception",
	                        JOptionPane.ERROR_MESSAGE);
	            }
	        }

	        if (table == null) {
	            // create new instance (otherwise it
	            // is inherited)
	            final String tn = unmarshallAttribute(tableNode, "name", "unknown");
	            final String type = unmarshallAttribute(tableNode, "type", "none");

	            if (tableNames.containsKey(tn) || type.contains("xis")) {
	                if (type.equalsIgnoreCase("3D")) {
	                    table = new Table3D();

	                } else if (type.equalsIgnoreCase("2D")) {
	                    table = new Table2D();

	                } else if (type.equalsIgnoreCase("1D")) {
	                    table = new Table1D(Table.TableType.TABLE_1D);

	                } else if (type.equalsIgnoreCase("X Axis")
	                        || type.equalsIgnoreCase("Static X Axis"))  {
	                    table = new Table1D(Table.TableType.X_AXIS);

	                } else if (type.equalsIgnoreCase("Y Axis")
	                        || type.equalsIgnoreCase("Static Y Axis")) {
	                    table = new Table1D(Table.TableType.Y_AXIS);
	                } else if (type.equalsIgnoreCase("Switch")) {
	                    table = new TableSwitch();

	                } else if (type.equalsIgnoreCase("BitwiseSwitch")) {
	                    table = new TableBitwiseSwitch();
	                }
	                else if(type.equalsIgnoreCase("none")){
	                    throw new XMLParseException("Table type unspecified for "
	                            + tableNode.getAttributes().getNamedItem("name"));
	                }
	                else {
	                    throw new XMLParseException("Table type " + type + " unknown for "
	                            + tableNode.getAttributes().getNamedItem("name"));
	                }	                
                    table.getScales().add(rawScale);
	            }
	            else {
	                return table;
	            }
	        }

	        // unmarshall table attributes
	        final String tn = unmarshallAttribute(tableNode, "name", table.getName());
	        table.setName(tn);
	        if (unmarshallAttribute(tableNode, "beforeram", "false")
	                .equalsIgnoreCase("true")) {
	            table.setBeforeRam(true);
	        }        
	        
	        
	        table.setDataLayout(unmarshallAttribute(tableNode, "dataLayout", ""));        
	        table.setCategory(unmarshallAttribute(tableNode, "category",
	                table.getCategory()));
	        if (table.getStorageType() < 1) {
	            table.setSignedData(RomAttributeParser
	                    .parseStorageDataSign(unmarshallAttribute(tableNode,
	                            "storagetype",
	                            String.valueOf(table.getStorageType()))));
	        }
	        table.setStorageType(RomAttributeParser
	                .parseStorageType(unmarshallAttribute(tableNode, "storagetype",
	                        String.valueOf(table.getStorageType()))));
	        if (memModelEndian == null) {
	            table.setEndian(RomAttributeParser.parseEndian(unmarshallAttribute(
	                    tableNode, "endian", table.getEndian().getMarshallingString())));
	        }
	        else {
	            final Settings.Endian endian = memModelEndian.equalsIgnoreCase("little") ? Settings.Endian.LITTLE : Settings.Endian.BIG;
	            table.setMemModelEndian(endian);
	            table.setEndian(endian);
	        }
	        if (tableNames.containsKey(tn)) {
	            table.setStorageAddress(tableNames.get(tn));
	        }
	        else {
	            table.setStorageAddress(RomAttributeParser
	                .parseHexString(unmarshallAttribute(tableNode,
	                        "storageaddress",
	                        String.valueOf(table.getStorageAddress()))));
	        }
	        
	        table.setDescription(unmarshallAttribute(tableNode, "description",
	                table.getDescription()));
	        table.setDataSize(unmarshallAttribute(tableNode, "sizey",
	                unmarshallAttribute(tableNode, "sizex", table.getDataSize())));
	        table.setFlip(unmarshallAttribute(tableNode, "flipy",
	                unmarshallAttribute(tableNode, "flipx", table.getFlip())));
	        table.setUserLevel(unmarshallAttribute(tableNode, "userlevel",
	                table.getUserLevel()));
	        table.setLocked(unmarshallAttribute(tableNode, "locked",
	                table.isLocked()));
	        table.setLogParam(unmarshallAttribute(tableNode, "logparam",
	                table.getLogParam()));
	        table.setStringMask(
	                unmarshallAttribute(tableNode, "mask", "0"));

	        if (table.getType() == Table.TableType.TABLE_3D) {
	            ((Table3D) table).setSwapXY(unmarshallAttribute(tableNode,
	                    "swapxy", ((Table3D) table).getSwapXY()));
	            ((Table3D) table).setFlipX(unmarshallAttribute(tableNode, "flipx",
	                    ((Table3D) table).getFlipX()));
	            ((Table3D) table).setFlipY(unmarshallAttribute(tableNode, "flipy",
	                    ((Table3D) table).getFlipY()));
	            ((Table3D) table).setSizeX(unmarshallAttribute(tableNode, "sizex",
	                    ((Table3D) table).getSizeX()));
	            ((Table3D) table).setSizeY(unmarshallAttribute(tableNode, "sizey",
	                    ((Table3D) table).getSizeY()));
	        }
	              
	        Node n;
	        NodeList nodes = tableNode.getChildNodes();

	        for (int i = 0; i < nodes.getLength(); i++) {
	            n = nodes.item(i);

	            if (n.getNodeType() == ELEMENT_NODE) {
	                if (n.getNodeName().equalsIgnoreCase("table")) {

	                    if (table.getType() == Table.TableType.TABLE_2D) { // if table is 2D,
	                        // parse axis

	                        if (RomAttributeParser
	                                .parseTableType(unmarshallAttribute(n, "type",
	                                        "unknown")) == Table.TableType.Y_AXIS
	                                        || RomAttributeParser
	                                        .parseTableType(unmarshallAttribute(n,
	                                                "type", "unknown")) == Table.TableType.X_AXIS) {

	                            Table1D tempTable = (Table1D) unmarshallTable(n,
	                                    ((Table2D) table).getAxis(), rom);
	                            if (tempTable.getDataSize() != table.getDataSize()) {
	                                tempTable.setDataSize(table.getDataSize());
	                            }
	                            ((Table2D) table).setAxis(tempTable);
	                            tempTable.setData(((Table2D) table).getAxis().getData());

	                        }
	                    } else if (table.getType() == Table.TableType.TABLE_3D) { // if table
	                        // is 3D,
	                        // populate
	                        // xAxis
	                        if (RomAttributeParser
	                                .parseTableType(unmarshallAttribute(n, "type",
	                                        "unknown")) == Table.TableType.X_AXIS) {

	                            Table1D tempTable = (Table1D) unmarshallTable(n,
	                                    ((Table3D) table).getXAxis(), rom);
	                            if (tempTable.getDataSize() != ((Table3D) table)
	                                    .getSizeX()) {
	                                tempTable.setDataSize(((Table3D) table)
	                                        .getSizeX());
	                            }
	                            
	                            ((Table3D) table).setXAxis(tempTable);
	                            tempTable.setData(((Table3D) table).getXAxis().getData());

	                        } else if (RomAttributeParser
	                                .parseTableType(unmarshallAttribute(n, "type",
	                                        "unknown")) == Table.TableType.Y_AXIS) {

	                            Table1D tempTable = (Table1D) unmarshallTable(n,
	                                    ((Table3D) table).getYAxis(), rom);
	                            if (tempTable.getDataSize() != ((Table3D) table)
	                                    .getSizeY()) {
	                                tempTable.setDataSize(((Table3D) table)
	                                        .getSizeY());
	                            }
	                            ((Table3D) table).setYAxis(tempTable);
	                            tempTable.setData(((Table3D) table).getYAxis()
	                                    .getData());


	                        }
	                    }

	                } else if (n.getNodeName().equalsIgnoreCase("scaling")) {
	                    // check whether scale already exists. if so, modify, else
	                    // use new instance
	                    Scale baseScale = table.getScale(unmarshallAttribute(n,"name", "Default"));
	                    table.addScale(unmarshallScale(n, baseScale));

	                } else if (n.getNodeName().equalsIgnoreCase("data")) {
	                    // parse and add data to table
	                    DataCell c = new DataCell(table, unmarshallText(n));
	                    if(table instanceof Table1D) {
	                        ((Table1D)table).addStaticDataCell(c);
	                    } else {
	                        // Why would this happen.  Static should only be for axis.
	                        LOGGER.error("Error adding static data cell.");
	                    }

	                } else if (n.getNodeName().equalsIgnoreCase("description")) {
	                    table.setDescription(unmarshallText(n));

	                } else if (n.getNodeName().equalsIgnoreCase("state")) {
	                	 table.setValues(
		                            unmarshallAttribute(n, "name", ""),
		                            unmarshallAttribute(n, "data", "0"));                                  
	                } else if (n.getNodeName().equalsIgnoreCase("bit")) {
	                    table.setValues(
	                            unmarshallAttribute(n, "name", ""),
	                            unmarshallAttribute(n, "position", "0"));
	 
	                } else { /* unexpected element in Table (skip) */
	                }
	            } else { /* unexpected node-type in Table (skip) */
	            }
	        }
	        return table;
	    }
	 
	    /**
	     * Create a list of table names to be used as a filter on the inherited
	     * tables to reduce unnecessary table object creation.
	     * @param nodes -  the NodeList to filter
	     * @throws XMLParseException
	     * @throws TableIsOmittedException
	     * @throws Exception
	     */
	    public void filterFoundRomTables (NodeList nodes) {
	        Node n;

	        for (int i = 0; i < nodes.getLength(); i++) {
	            n = nodes.item(i);
	            if (n.getNodeType() == ELEMENT_NODE
	                    && n.getNodeName().equalsIgnoreCase("table")) {

	                final String name = unmarshallAttribute(n, "name", "unknown");
	                final int address = RomAttributeParser
	                        .parseHexString(unmarshallAttribute(n,
	                            "storageaddress", "-1"));

	                if (unmarshallAttribute(n, "omit", "false").equalsIgnoreCase(
	                        "true")) {
	                    return;
	                }

	                if (!tableNames.containsKey(name) && address >= 0) {
	                    tableNames.put(name, address);
	                }
	                else if (tableNames.containsKey(name)) {
	                    if (tableNames.get(name) < 1 && address >= 0) {
	                        tableNames.put(name, address);
	                        }
	                }
	            }
	        }
	    }
	 
	    public Scale unmarshallScale(Node scaleNode, Scale scale) {

	        // look for base scale first
	        String base = unmarshallAttribute(scaleNode, "base", "none");
	        if (!base.equalsIgnoreCase("none")) {
	            for (Scale scaleItem : scales) {

	                // check whether name matches base and set scale if so
	                if (scaleItem.getName().equalsIgnoreCase(base)) {
	                    try {
	                        scale = (Scale) ObjectCloner.deepCopy(scaleItem);

	                    } catch (Exception ex) {
	                        JOptionPane.showMessageDialog(
	                                ECUEditorManager.getECUEditor(),
	                                new DebugPanel(ex, SettingsManager.getSettings()
	                                        .getSupportURL()), "Exception",
	                                        JOptionPane.ERROR_MESSAGE);
	                    }
	                }
	            }
	        }

	        // set remaining attributes
	        scale.setName(unmarshallAttribute(scaleNode, "name", "Default"));
	        scale.setUnit(unmarshallAttribute(scaleNode, "units", scale.getUnit()));
	        scale.setExpression(unmarshallAttribute(scaleNode, "expression",
	                scale.getExpression()));
	        scale.setByteExpression(unmarshallAttribute(scaleNode, "to_byte",
	                scale.getByteExpression()));
	        scale.setFormat(unmarshallAttribute(scaleNode, "format", "#"));
	        scale.setMax(unmarshallAttribute(scaleNode, "max", 0.0));
	        scale.setMin(unmarshallAttribute(scaleNode, "min", 0.0));

	        // get coarse increment with new attribute name (coarseincrement), else
	        // look for old (increment)
	        scale.setCoarseIncrement(unmarshallAttribute(
	                scaleNode,
	                "coarseincrement",
	                unmarshallAttribute(scaleNode, "increment",
	                        scale.getCoarseIncrement())));

	        scale.setFineIncrement(unmarshallAttribute(scaleNode, "fineincrement",
	                scale.getFineIncrement()));
	        for (Scale s : scales) {
	            if (s.equals(scale)) {
	                return s;
	            }
	        }
	        scales.add(scale);
	        return scale;
	    }
}
