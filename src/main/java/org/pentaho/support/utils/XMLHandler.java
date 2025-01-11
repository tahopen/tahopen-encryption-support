/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2020 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.support.utils;


import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;

/**
 * This class contains a number of (static) methods to facilitate the retrieval of information from XML Node(s).
 *
 * @author Matt
 * @since 04-04-2003
 */
public class XMLHandler {

  /**
   * Load file input stream into an XML document
   *
   * @param inputStream    The stream to load a document from
   * @param namespaceAware support XML namespaces.
   * @return the Document if all went well, null if an error occured!
   */
  public static Document loadXMLFile( InputStream inputStream, boolean namespaceAware ) throws XmlParseException {
    try {
      // Check and open XML document
      //
      DocumentBuilderFactory dbf = createSecureDocBuilderFactory();
      dbf.setIgnoringComments( true );
      dbf.setNamespaceAware( namespaceAware );
      DocumentBuilder db = dbf.newDocumentBuilder();

      // even dbf.setValidating(false) will the parser NOT prevent from checking the existance of the DTD
      // thus we need to give the BaseURI (systemID) below to have a chance to get it
      // or return empty dummy documents for all external entities (sources)
      //

      Document doc;
      try {
        // Normal parsing
        //
        doc = db.parse( inputStream );
      } finally {
        if ( inputStream != null ) {
          inputStream.close();
        }
      }

      return doc;
    } catch ( Exception e ) {
      throw new XmlParseException( "Error reading information from input stream", e );
    }
  }

  /**
   * Search for a subnode in the node with a certain tag.
   *
   * @param n   The node to look in
   * @param tag The tag to look for
   * @return The subnode if the tag was found, or null if nothing was found.
   */
  public static Node getSubNode( Node n, String tag ) {
    int i;
    NodeList children;
    Node childnode;

    if ( n == null ) {
      return null;
    }

    // Get the children one by one out of the node,
    // compare the tags and return the first found.
    //
    children = n.getChildNodes();
    for ( i = 0; i < children.getLength(); i++ ) {
      childnode = children.item( i );
      if ( childnode.getNodeName().equalsIgnoreCase( tag ) ) {
        return childnode;
      }
    }
    return null;
  }

  /**
   * Get nodes with a certain tag one level down
   *
   * @param n   The node to look in
   * @param tag The tags to count
   * @return The list of nodes found with the specified tag
   */
  public static List<Node> getNodes( Node n, String tag ) {
    NodeList children;
    Node childnode;

    List<Node> nodes = new ArrayList<Node>();

    if ( n == null ) {
      return nodes;
    }

    children = n.getChildNodes();
    for ( int i = 0; i < children.getLength(); i++ ) {
      childnode = children.item( i );
      if ( childnode.getNodeName().equalsIgnoreCase( tag ) ) {
        // <file>
        nodes.add( childnode );
      }
    }
    return nodes;
  }

  public static String getTagAttribute( Node node, String attribute ) {
    if ( node == null ) {
      return null;
    }

    String retval = null;

    NamedNodeMap nnm = node.getAttributes();
    if ( nnm != null ) {
      Node attr = nnm.getNamedItem( attribute );
      if ( attr != null ) {
        retval = attr.getNodeValue();
      }
    }
    return retval;
  }

  /**
   * Get the value of a tag in a node
   *
   * @param n   The node to look in
   * @param tag The tag to look for
   * @return The value of the tag or null if nothing was found.
   */
  public static String getTagValue( Node n, String tag ) {
    NodeList children;
    Node childnode;

    if ( n == null ) {
      return null;
    }

    children = n.getChildNodes();
    for ( int i = 0; i < children.getLength(); i++ ) {
      childnode = children.item( i );
      if ( childnode.getNodeName().equalsIgnoreCase( tag ) ) {
        if ( childnode.getFirstChild() != null ) {
          return childnode.getFirstChild().getNodeValue();
        }
      }
    }
    return null;
  }

  /**
   * Creates an instance of {@link DocumentBuilderFactory} class with enabled {@link
   * XMLConstants#FEATURE_SECURE_PROCESSING} property. Enabling this feature prevents from some XXE attacks (e.g. XML
   * bomb) See PPP-3506 for more details.
   *
   * @throws ParserConfigurationException if feature can't be enabled
   */
  public static DocumentBuilderFactory createSecureDocBuilderFactory() throws ParserConfigurationException {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilderFactory.setFeature( FEATURE_SECURE_PROCESSING, true );
    docBuilderFactory.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );

    return docBuilderFactory;
  }
}
