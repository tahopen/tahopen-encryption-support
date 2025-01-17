package org.pentaho.support.encryption;
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

import org.pentaho.di.core.encryption.TwoWayPasswordEncoderInterface;
import org.pentaho.support.utils.StringUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles basic encryption of passwords in Kettle. Note that it's not really encryption, it's more
 * obfuscation. Passwords are <b>difficult</b> to read, not impossible.
 *
 * @author Matt
 * @since 17-12-2003
 *
 */
public class KettleTwoWayPasswordEncoder implements TwoWayPasswordEncoderInterface {
  private static final KettleTwoWayPasswordEncoder instance = new KettleTwoWayPasswordEncoder();
  private static final int RADIX = 16;
  private String Seed;
  /**
   * The word that is put before a password to indicate an encrypted form. If this word is not present, the password is
   * considered to be NOT encrypted
   */
  @SuppressWarnings( "squid:S2068" ) public static final String PASSWORD_ENCRYPTED_PREFIX = "Encrypted ";

  public KettleTwoWayPasswordEncoder() {
    String envSeed = System.getProperty( Encr.KETTLE_TWO_WAY_PASSWORD_ENCODER_SEED, "0933910847463829827159347601486730416058" );
    Seed = envSeed;
  }

  public void init() throws PasswordEncoderException {
    // Nothing to do here.
  }

  public String encode( String rawPassword ) {
    return encode( rawPassword, true );
  }

  public String encode( String rawPassword, boolean includePrefix ) {
    if ( includePrefix ) {
      return encryptPasswordIfNotUsingVariablesInternal( rawPassword );
    } else {
      return encryptPasswordInternal( rawPassword );
    }
  }

  public String decode( String encodedPassword ) {

    if ( encodedPassword != null && encodedPassword.startsWith( PASSWORD_ENCRYPTED_PREFIX ) ) {
      encodedPassword = encodedPassword.substring( PASSWORD_ENCRYPTED_PREFIX.length() );
    }

    return decryptPasswordInternal( encodedPassword );
  }

  public String decode( String encodedPassword, boolean optionallyEncrypted ) {

    if ( encodedPassword == null ) {
      return null;
    }

    if ( optionallyEncrypted ) {

      if ( encodedPassword.startsWith( PASSWORD_ENCRYPTED_PREFIX ) ) {
        encodedPassword = encodedPassword.substring( PASSWORD_ENCRYPTED_PREFIX.length() );
        return decryptPasswordInternal( encodedPassword );
      } else {
        return encodedPassword;
      }
    } else {
      return decryptPasswordInternal( encodedPassword );
    }
  }

  protected String encryptPasswordInternal( String password ) {
    if ( password == null ) {
      return "";
    }
    if ( password.length() == 0 ) {
      return "";
    }

    BigInteger bi_passwd = new BigInteger( password.getBytes() );

    BigInteger bi_r0 = new BigInteger( getSeed() );
    BigInteger bi_r1 = bi_r0.xor( bi_passwd );

    return bi_r1.toString( RADIX );
  }

  protected String decryptPasswordInternal( String encrypted ) {
    if ( encrypted == null ) {
      return "";
    }
    if ( encrypted.length() == 0 ) {
      return "";
    }

    BigInteger bi_confuse = new BigInteger( getSeed() );

    try {
      BigInteger bi_r1 = new BigInteger( encrypted, RADIX );
      BigInteger bi_r0 = bi_r1.xor( bi_confuse );

      return new String( bi_r0.toByteArray() );
    } catch ( Exception e ) {
      return "";
    }
  }

  protected String getSeed() {
    return this.Seed;
  }

  public String[] getPrefixes() {
    return new String[] { PASSWORD_ENCRYPTED_PREFIX };
  }


  /**
   * Encrypt the password, but only if the password doesn't contain any variables.
   *
   * @param password
   *          The password to encrypt
   * @return The encrypted password or the
   */
  protected final String encryptPasswordIfNotUsingVariablesInternal( String password ) {
    String encrPassword = "";
    List<String> varList = new ArrayList<>();
    StringUtil.getUsedVariables( password, varList, true );
    if ( varList.isEmpty() ) {
      encrPassword = PASSWORD_ENCRYPTED_PREFIX + encryptPasswordInternal( password );
    } else {
      encrPassword = password;
    }

    return encrPassword;
  }


  /**
   * Decrypts a password if it contains the prefix "Encrypted "
   *
   * @param password
   *          The encrypted password
   * @return The decrypted password or the original value if the password doesn't start with "Encrypted "
   */
  protected final String decryptPasswordOptionallyEncryptedInternal( String password ) {
    if ( !StringUtil.isEmpty( password ) && password.startsWith( PASSWORD_ENCRYPTED_PREFIX ) ) {
      return decryptPasswordInternal( password.substring( PASSWORD_ENCRYPTED_PREFIX.length() ) );
    }
    return password;
  }

}
