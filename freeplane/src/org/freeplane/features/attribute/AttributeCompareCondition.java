/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.attribute;

import org.freeplane.core.io.xml.TreeXmlReader;
import org.freeplane.core.io.xml.TreeXmlWriter;
import org.freeplane.core.util.TypeReference;
import org.freeplane.features.filter.condition.ASelectableCondition;
import org.freeplane.features.filter.condition.CompareConditionAdapter;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.freeplane.n3.nanoxml.XMLElement;

/**
 * @author Dimitry Polivaev
 */
public class AttributeCompareCondition extends CompareConditionAdapter {
	static final String ATTRIBUTE = "ATTRIBUTE";
	static final String COMPARATION_RESULT = "COMPARATION_RESULT";
	static final String NAME = "attribute_compare_condition";
	static final String SUCCEED = "SUCCEED";

	static ASelectableCondition load(final XMLElement element) {
		final String attr = element.getAttribute(AttributeCompareCondition.ATTRIBUTE, null);
		Object value = element.getAttribute(CompareConditionAdapter.VALUE, null);
		if(value == null){
			final String spec = element.getAttribute(CompareConditionAdapter.OBJECT, null);
			value = TypeReference.create(spec);
		}
			
		final boolean matchCase = TreeXmlReader.xmlToBoolean(element.getAttribute(
		    CompareConditionAdapter.MATCH_CASE, null));
		final int compResult = Integer.parseInt(element.getAttribute(
		    AttributeCompareCondition.COMPARATION_RESULT, null));
		final boolean succeed = TreeXmlReader.xmlToBoolean(element.getAttribute(
		    AttributeCompareCondition.SUCCEED, null));
		return new AttributeCompareCondition(attr, value, matchCase, compResult, succeed);
	}

	final private String attribute;
	final private int comparationResult;
	final private boolean succeed;

	/**
	 */
	public AttributeCompareCondition(final String attribute, final Object value, final boolean matchCase,
	                                 final int comparationResult, final boolean succeed) {
		super(value, matchCase);
		this.attribute = attribute;
		this.comparationResult = comparationResult;
		this.succeed = succeed;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * freeplane.controller.filter.condition.Condition#checkNode(freeplane.modes
	 * .MindMapNode)
	 */
	public boolean checkNode(final NodeModel node) {
		final IAttributeTableModel attributes = NodeAttributeTableModel.getModel(node);
		final TextController textController = TextController.getController();
		for (int i = 0; i < attributes.getRowCount(); i++) {
			try {
				if(! attributes.getValueAt(i, 0).equals(attribute)) {
					continue;
				}
			    final Object originalContent = attributes.getValueAt(i, 1);
				final Object text = textController.getTransformedObject(originalContent, node, null);
				compareTo(text);
				return isComparisonOK() &&  succeed == (getComparisonResult() == comparationResult);
			}
			catch (final NumberFormatException fne) {
			}
		}
		return false;
	}

	@Override
	protected String createDescription() {
		return super.createDescription(attribute, comparationResult, succeed);
	}

	public void fillXML(final XMLElement child) {
		super.fillXML(child);
		child.setAttribute(AttributeCompareCondition.ATTRIBUTE, attribute);
		child.setAttribute(AttributeCompareCondition.COMPARATION_RESULT, Integer.toString(comparationResult));
		child.setAttribute(AttributeCompareCondition.SUCCEED, TreeXmlWriter.BooleanToXml(succeed));
	}

	@Override
    protected String getName() {
	    return NAME;
    }
}