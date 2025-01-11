package org.pentaho.support.encryption;

/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2020 by Hitachi Vantara : http://www.pentaho.com
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

public class PasswordEncoderException extends Exception {
  private static final String CR = System.getProperty( "line.separator" );

  /**
   * Constructs a new throwable with null as its detail message.
   */
  public PasswordEncoderException() {
    super();
  }

  /**
   * Constructs a new throwable with the specified detail message.
   *
   * @param message
   *          - the detail message. The detail message is saved for later retrieval by the getMessage() method.
   */
  public PasswordEncoderException( String message ) {
    super( message );
  }

  /**
   * @param message
   * @param cause
   */
  public PasswordEncoderException( String message, Throwable cause ) {
    super( message, cause );
  }

  @Override
  public String getMessage() {
    StringBuilder retval = new StringBuilder();
    retval.append( CR );
    retval.append( super.getMessage() ).append( CR );

    Throwable cause = getCause();
    if ( cause != null ) {
      String message = cause.getMessage();
      if ( message != null ) {
        retval.append( message ).append( CR );
      } else {
        // Add with stack trace elements of cause...
        StackTraceElement[] ste = cause.getStackTrace();
        for ( int i = ste.length - 1; i >= 0; i-- ) {
          retval.append( " at " ).append( ste[ i ].getClassName() ).append( "." ).append( ste[ i ].getMethodName() )
            .append( " (" ).append( ste[ i ].getFileName() )
            .append( ":" ).append( ste[ i ].getLineNumber() ).append( ")" ).append( CR );

        }
      }
    }

    return retval.toString();
  }

}
