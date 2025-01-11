package org.pentaho.support.encryption;

/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2021 by Hitachi Vantara : http://www.pentaho.com
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

import org.eclipse.jetty.util.security.Password;
import org.pentaho.di.core.encryption.TwoWayPasswordEncoderInterface;
import org.pentaho.support.utils.StringUtil;
import org.pentaho.support.utils.XMLHandler;
import org.pentaho.support.utils.XmlParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles basic encryption of passwords in Kettle. Note that it's not really encryption, it's more
 * obfuscation. Passwords are <b>difficult</b> to read, not impossible.
 *
 * @author Matt
 * @since 17-12-2003
 */
public class Encr {

  public static final String KETTLE_PASSWORD_ENCODER_PLUGINS_FILE = "KETTLE_PASSWORD_ENCODER_PLUGINS_FILE";
  public static final String KETTLE_PASSWORD_ENCODER_PLUGIN = "KETTLE_PASSWORD_ENCODER_PLUGIN";
  public static final String KETTLE_TWO_WAY_PASSWORD_ENCODER_SEED = "KETTLE_TWO_WAY_PASSWORD_ENCODER_SEED";
  public static final String XML_FILE_KETTLE_PASSWORD_ENCODER_PLUGINS = "kettle-password-encoder-plugins.xml";

  private static final String FILE_SEPARATOR = System.getProperty( "file.separator" );

  protected static boolean isJunitTest;
  // Tells the class not to truely exit in command line mode because it is running in unit test
  protected static int exitCode; // The exit code when running junit tests

  /**
   * The word that is put before a password to indicate an encrypted form. If this word is not present, the password is
   * considered to be NOT encrypted
   */
  @SuppressWarnings( "squid:S2068" ) public static final String PASSWORD_ENCRYPTED_PREFIX = "Encrypted ";

  protected static Encr instance;
  //Legacy code used "kettle" in encr command and "Kettle" in plugin so had to make case insensitive
  private Map<String, TwoWayPasswordEncoderInterface> encoderMap =
    new HashMap<String, TwoWayPasswordEncoderInterface>();

  private String defaultEncoderId;
  private String firstId; //If no Id is explicitly marked as default then we use the first one defined

  private Encr() {
  }

  public static Encr getInstance() throws PasswordEncoderException, XmlParseException {
    if ( instance == null ) {
      Encr encr = new Encr();
      encr.setupPasswordEncoders();
      instance = encr;
    }
    return instance;
  }

  public String encryptPassword( String password ) {
    return encryptPassword( getDefaultEncoderId(), password );
  }

  public String encryptPassword( String encoderId, String password ) {
    return getEncoder( encoderId ).encode( password, false );
  }

  public String decryptPassword( String password ) {
    return decryptPassword( getDefaultEncoderId(), password );
  }

  public String decryptPassword( String encoderId, String encrypted ) {
    return getEncoder( encoderId ).decode( encrypted );
  }

  public String encryptPasswordIfNotUsingVariables( String password ) {
    return encryptPasswordIfNotUsingVariables( getDefaultEncoderId(), password );
  }

  /**
   * Encrypt the password, but only if the password doesn't contain any variables.
   *
   * @param password The password to encrypt
   * @return The encrypted password or the
   */
  public String encryptPasswordIfNotUsingVariables( String encoderId, String password ) {
    return getEncoder( encoderId ).encode( password, true );
  }

  public String decryptPasswordOptionallyEncrypted( String password ) {
    return decryptPasswordOptionallyEncrypted( getDefaultEncoderId(), password );
  }

  /**
   * Decrypts a password if it contains the prefix "Encrypted "
   *
   * @param password The encrypted password
   * @return The decrypted password or the original value if the password doesn't start with "Encrypted "
   */
  public String decryptPasswordOptionallyEncrypted( String encoderId, String password ) {

    return getEncoder( encoderId ).decode( password, true );
  }

  /**
   * Create an encrypted password
   *
   * @param args the password to encrypt
   */
  public static void main( String[] args ) throws PasswordEncoderException, XmlParseException {
    Encr encr = Encr.getInstance();
    if ( args.length < 1 || args.length > 2 ) {
      printOptions();
      if ( exitIfNotTest( 9 ) ) {
        return;
      }
    }

    String option;
    String password;

    if ( args.length == 2 ) {
      option = args[ 0 ].trim().substring( 1 ).toLowerCase();
      password = args[ 1 ];
    } else {
      option = instance.defaultEncoderId;
      password = args[ 0 ];
    }

    if ( option.equalsIgnoreCase( "carte" ) ) {
      // Jetty password obfuscation
      //
      String obfuscated = Password.obfuscate( password );
      System.out.println( obfuscated );
      if ( exitIfNotTest( 0 ) ) {
        return;
      }
    } else if ( instance.encoderMap.get( option ) != null ) {
      // Kettle or other password obfuscation
      //
      try {
        String obfuscated = encr.encryptPasswordIfNotUsingVariables( option, password );
        System.out.println( obfuscated );
        if ( exitIfNotTest( 0 ) ) {
          return;
        }
      } catch ( Exception ex ) {
        System.err.println( "Error encrypting password" );
        ex.printStackTrace();
        if ( exitIfNotTest( 2 ) ) {
          return;
        }
      }

    } else {
      // Unknown option, print usage
      //
      System.err.println( "Unknown option '" + option + "'\n" );
      printOptions();
      if ( exitIfNotTest( 1 ) ) {
        return;
      }
    }

  }

  private void setupPasswordEncoders() throws PasswordEncoderException {
    String xmlFile = XML_FILE_KETTLE_PASSWORD_ENCODER_PLUGINS;
    String alternative = StringUtil.NVL( System.getProperty( KETTLE_PASSWORD_ENCODER_PLUGINS_FILE ), null );
    boolean registeredDefault;
    boolean registeredAlternative;

    // Load the plugins for the default file...
    try {
      registeredDefault = registerPlugins( xmlFile );
    } catch ( Exception e ) {
      throw new PasswordEncoderException( "Unable to load native plugins '" + xmlFile + "'", e );
    }
    try {
      registeredAlternative = registerPlugins( alternative );
    } catch ( Exception e ) {
      throw new PasswordEncoderException( "Unable to load alternative plugins '" + alternative + "'", e );
    }
    if ( !registeredDefault && !registeredAlternative ) {
      throw new PasswordEncoderException(
        "Unable to load a defining plugin xml file for TwoWayPasswordEncoderInteface.  Please create file '"
          + XML_FILE_KETTLE_PASSWORD_ENCODER_PLUGINS + "'" );
    }
  }


  private boolean registerPlugins( String xmlFile ) throws PasswordEncoderException, XmlParseException {
    if ( !StringUtil.isEmpty( xmlFile ) ) {
      InputStream inputStream = getResAsStreamExternal( xmlFile );
      if ( inputStream == null ) {
        inputStream = getResAsStreamExternal( "/" + xmlFile );
      }
      if ( inputStream != null ) {
        registerPlugins( inputStream );
        if ( defaultEncoderId == null ) {
          defaultEncoderId = firstId; //If no encoders were marked as default, make the first one the default one
        }
        return true;
      }
    }
    return false;
  }

  private void registerPlugins( InputStream inputStream ) throws PasswordEncoderException, XmlParseException {
    try {
      Document document = XMLHandler.loadXMLFile( inputStream, false );

      Node repsNode = XMLHandler.getSubNode( document, "password-encoder-plugins" );
      List<Node> repsNodes = XMLHandler.getNodes( repsNode, "password-encoder-plugin" );

      for ( Node repNode : repsNodes ) {
        registerPluginFromXmlResource( repNode );
      }
    } finally {
      try {
        if ( inputStream != null ) {
          inputStream.close();
        }
      } catch ( IOException e ) {
        //close quietly
      }
    }
  }

  protected void registerPluginFromXmlResource( Node pluginNode )
    throws PasswordEncoderException {

    String idTag = XMLHandler.getTagAttribute( pluginNode, "id" );
    String classname = getTagOrAttribute( pluginNode, "classname" );
    String defaultTag = getTagOrAttribute( pluginNode, "default-encoder" );
    String seedXML = getTagOrAttribute( pluginNode, "seed" );
    if ( seedXML != null ) {
      System.setProperty( KETTLE_TWO_WAY_PASSWORD_ENCODER_SEED, seedXML );
    }

    boolean isDefault = ( defaultTag != null && ( defaultTag.toLowerCase().startsWith( "t" ) || defaultTag.toLowerCase()
      .startsWith( "y" ) ) );

    try {
      Class clazz = Class.forName( classname );
      TwoWayPasswordEncoderInterface encoder = (TwoWayPasswordEncoderInterface) clazz.newInstance();
      encoder.init();
      String id = idTag.toLowerCase();
      encoderMap.put( id, encoder );
      if ( isDefault ) {
        if ( defaultEncoderId != null ) {
          throw new PasswordEncoderException( "Only one encoder can be marked as \"default-encoder\"" );
        }
        defaultEncoderId = id;
      }
      if ( firstId == null ) {
        firstId = id;
      }
    } catch ( ClassNotFoundException e ) {
      throw new PasswordEncoderException( "ClassNotFound: " + classname );
    } catch ( InstantiationException | IllegalAccessException e ) {
      throw new PasswordEncoderException( "Could not instantiate: " + classname );
    }

  }

  private String getTagOrAttribute( Node pluginNode, String tag ) {
    String string = XMLHandler.getTagValue( pluginNode, tag );
    if ( string == null ) {
      string = XMLHandler.getTagAttribute( pluginNode, tag );
    }
    return string;
  }

  private InputStream getResAsStreamExternal( String name ) {
    return getClass().getResourceAsStream( name );
  }

  private InputStream getFileInputStreamExternal( String name ) throws FileNotFoundException {
    return new FileInputStream( name );
  }

  private TwoWayPasswordEncoderInterface getEncoder( String encoderId ) {
    TwoWayPasswordEncoderInterface encoder = instance.encoderMap.get( encoderId );
    if ( encoder == null ) {
      throw new RuntimeException( "plugin id '" + encoderId + "' does not exist" );
    }
    return encoder;
  }

  private String getDefaultEncoderId() {
    return StringUtil.NVL( System.getProperty( KETTLE_PASSWORD_ENCODER_PLUGIN ), defaultEncoderId ).toLowerCase();
  }

  private static void printOptions() {
    System.err.println( "encr usage:\n" );
    System.err.println( "  encr <-kettle|-carte> <password>" );
    System.err.println( "  Options:" );
    System.err.println( "    -kettle: generate an obfuscated password to include in Kettle XML files" );
    System.err
      .println( "    -carte : generate an obfuscated password to include in the carte password file 'pwd/kettle.pwd'" );
    System.err
      .println( "\nThis command line tool obfuscates a plain text password for use in XML and password files." );
    System.err.println( "Make sure to also copy the '" + PASSWORD_ENCRYPTED_PREFIX
      + "' prefix to indicate the obfuscated nature of the password." );
    System.err.println(
      "Kettle will then be able to make the distinction between regular plain text passwords and obfuscated ones." );
    System.err.println();
  }

  private static boolean exitIfNotTest( int exitCode ) {
    // If running in junit test set the exit code nd return true, else do true system exit
    if ( isJunitTest ) {
      Encr.exitCode = exitCode;
    } else {
      System.exit( exitCode );
    }
    return true;
  }
}
