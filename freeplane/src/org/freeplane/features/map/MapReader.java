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
package org.freeplane.features.map;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import org.freeplane.core.io.IElementDOMHandler;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.UnknownElements;
import org.freeplane.core.io.xml.TreeXmlReader;
import org.freeplane.features.map.MapWriter.Hint;
import org.freeplane.features.map.MapWriter.Mode;
import org.freeplane.n3.nanoxml.XMLElement;
import org.freeplane.n3.nanoxml.XMLException;

/**
 * @author Dimitry Polivaev
 * 20.12.2008
 */
public class MapReader implements IElementDOMHandler {
	public class NodeTreeCreator {
		private MapModel createdMap;
		private final HashMap<Object, Object> hints;
		private NodeModel mapChild = null;
		private final HashMap<String, String> newIds;

		public NodeTreeCreator() {
			super();
			newIds = new HashMap<String, String>();
			hints = new HashMap<Object, Object>();
		}

		public NodeModel create(final Reader pReader) throws XMLException {
			final NodeTreeCreator oldNodeTreeCreator = nodeTreeCreator;
			final TreeXmlReader reader = new TreeXmlReader(readManager);
			try {
				nodeTreeCreator = this;
				reader.load(createdMap, pReader);
				final NodeModel node = nodeBuilder.getMapChild();
				return node;
			}
			finally {
				nodeBuilder.reset();
				nodeTreeCreator = oldNodeTreeCreator;
			}
		}

		public NodeModel createNodeTreeFromXml(final MapModel map, final Reader pReader) throws IOException,
		        XMLException {
			start(map);
			final NodeModel node = create(pReader);
			if (node == null)
				throw new RuntimeException("corrupt map");
			finish(node);
			return node;
		}

		public void finish(final NodeModel node) {
			final NodeTreeCreator oldNodeTreeCreator = nodeTreeCreator;
			try {
				nodeTreeCreator = this;
				readManager.readingCompleted(node, newIds);
				newIds.clear();
				createdMap = null;
			}
			finally {
				nodeTreeCreator = oldNodeTreeCreator;
			}
		}

		void start(final MapModel map) {
			createdMap = map;
		}

		public MapModel getCreatedMap() {
			return createdMap;
		}

		Object getHint(final Object key) {
			return hints.get(key);
		}

		public void setHint(final Object key, final Object value) {
			hints.put(key, value);
		}

		NodeModel getMapChild() {
			return mapChild;
		}

		public void setMapChild(final NodeModel mapChild) {
			this.mapChild = mapChild;
		}

		public void substituteNodeID(final String value, final String realId) {
			newIds.put(value, realId);
		}
	}

	private final NodeBuilder nodeBuilder;
	final private ReadManager readManager;
	private NodeTreeCreator nodeTreeCreator;

	public NodeTreeCreator getCurrentNodeTreeCreator() {
		return nodeTreeCreator;
	}

	public MapReader(final ReadManager readManager) {
		this.readManager = readManager;
		nodeBuilder = new NodeBuilder(this);
		nodeBuilder.registerBy(readManager);
	}

	public Object createElement(final Object parent, final String tag, final XMLElement attributes) {
		return nodeTreeCreator.getCreatedMap();
	}

	public NodeModel createNodeTreeFromXml(final MapModel map, final Reader pReader, final Mode mode)
	        throws IOException, XMLException {
		final NodeTreeCreator oldNodeTreeCreator = nodeTreeCreator;
		try {
			nodeTreeCreator = new NodeTreeCreator();
			nodeTreeCreator.setHint(Hint.MODE, mode);
			final NodeModel topNode = nodeTreeCreator.createNodeTreeFromXml(map, pReader);
			return topNode;
		}
		finally {
			nodeTreeCreator = oldNodeTreeCreator;
		}
	}

	public void endElement(final Object parent, final String tag, final Object element, final XMLElement dom) {
		final MapModel map = (MapModel) element;
		if (dom.getAttributeCount() != 0 || dom.hasChildren()) {
			map.addExtension(new UnknownElements(dom));
		}
	}

	public boolean isMapLoadingInProcess() {
		return nodeTreeCreator != null;
	}

	public NodeTreeCreator nodeTreeCreator(final MapModel map) {
		final NodeTreeCreator nodeTreeCreator = new NodeTreeCreator();
		nodeTreeCreator.start(map);
		return nodeTreeCreator;
	}
}