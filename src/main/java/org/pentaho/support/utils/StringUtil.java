package org.pentaho.support.utils;

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

import java.util.Arrays;
import java.util.List;

/**
 * A collection of utilities to manipulate strings.
 *
 * @author wdeclerc
 */
public class StringUtil {
  private static final String UNIX_OPEN = "${";

  private static final String UNIX_CLOSE = "}";

  private static final String WINDOWS_OPEN = "%%";

  private static final String WINDOWS_CLOSE = "%%";

  private static final String[] SYSTEM_PROPERTIES = new String[] {
    "java.version", "java.vendor", "java.vendor.url", "java.home", "java.vm.specification.version",
    "java.vm.specification.vendor", "java.vm.specification.name", "java.vm.version", "java.vm.vendor",
    "java.vm.name", "java.specification.version", "java.specification.vendor", "java.specification.name",
    "java.class.version", "java.class.path", "java.library.path", "java.io.tmpdir", "java.compiler",
    "java.ext.dirs",

    "os.name", "os.arch", "os.version",

    "file.separator", "path.separator", "line.separator",

    "user.name", "user.home", "user.dir", "user.country", "user.language", "user.timezone",

    "org.apache.commons.logging.Log", "org.apache.commons.logging.simplelog.log.org.apache.http",
    "org.apache.commons.logging.simplelog.showdatetime", "org.eclipse.swt.browser.XULRunnerInitialized",
    "org.eclipse.swt.browser.XULRunnerPath",

    "sun.arch.data.model", "sun.boot.class.path", "sun.boot.library.path", "sun.cpu.endian", "sun.cpu.isalist",
    "sun.io.unicode.encoding", "sun.java.launcher", "sun.jnu.encoding", "sun.management.compiler",
    "sun.os.patch.level", };

  private StringUtil() {
    throw new IllegalStateException( "Utility Class" );
  }

  /**
   * Search the string and report back on the variables used in list
   *
   * @param aString                The string to search
   * @param list                   the list of variables to add to
   * @param includeSystemVariables also check for system variables.
   */
  public static void getUsedVariables( String aString, List<String> list, boolean includeSystemVariables ) {
    getUsedVariables( aString, UNIX_OPEN, UNIX_CLOSE, list, includeSystemVariables );
    getUsedVariables( aString, WINDOWS_OPEN, WINDOWS_CLOSE, list, includeSystemVariables );
  }

  /**
   * Search the string and report back on the variables used
   *
   * @param aString                The string to search
   * @param open                   the open or "start of variable" characters ${ or %%
   * @param close                  the close or "end of variable" characters } or %%
   * @param list                   the list of variables to add to
   * @param includeSystemVariables also check for system variables.
   */
  public static void getUsedVariables( String aString, String open, String close, List<String> list,
                                       boolean includeSystemVariables ) {
    if ( aString == null ) {
      return;
    }

    int p = 0;
    while ( p < aString.length() ) {
      // OK, we found something... : start of Unix variable
      if ( aString.startsWith( open, p ) ) {
        // See if it's closed...
        int from = p + open.length();
        int to = aString.indexOf( close, from + 1 );

        if ( to >= 0 ) {
          String variable = aString.substring( from, to );
          if ( list.indexOf( variable ) < 0 ) {
            // Either we include the system variables (all)
            // Or the variable is not a system variable
            // Or it's a system variable but the value has not been set (and we offer the user the option to set it)
            //
            if ( includeSystemVariables || Arrays.asList( SYSTEM_PROPERTIES ).indexOf( variable ) < 0
              || System.getProperty( variable ) == null ) {
              list.add( variable );
            }
          }
          // OK, continue
          p = to + close.length();
        }
      }
      p++;
    }
  }

  /**
   * Check if the CharSequence supplied is empty. A CharSequence is empty when it is null or when the length is 0
   *
   * @param val The stringBuffer to check
   * @return true if the stringBuffer supplied is empty
   */
  public static boolean isEmpty( CharSequence val ) {
    return val == null || val.length() == 0;
  }

  /**
   * Implements Oracle style NVL function
   *
   * @param source
   *          The source argument
   * @param def
   *          The default value in case source is null or the length of the string is 0
   * @return source if source is not null, otherwise return def
   */
  public static String NVL( String source, String def ) {
    if ( source == null || source.length() == 0 ) {
      return def;
    }
    return source;
  }
}

