/* -------------------------------------------------------------------------- */
/* - lucene-oxygen - custom indexer and searcher                            - */
/* - Copyright (C) 2018  <https://github.com/louiscyphre>,                  - */
/* -                     <https://github.com/TegraMorgan>,                  - */
/* -                     University of Haifa                                - */
/* -                                                                        - */
/* - This program is free software: you can redistribute it and/or modify   - */
/* - it under the terms of the GNU General Public License as published by   - */
/* - the Free Software Foundation, either version 3 of the License, or      - */
/* - (at your option) any later version.                                    - */
/* -                                                                        - */
/* - This program is distributed in the hope that it will be useful,        - */
/* - but WITHOUT ANY WARRANTY; without even the implied warranty of         - */
/* - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          - */
/* - GNU General Public License for more details.                           - */
/* -                                                                        - */
/* - You should have received a copy of the GNU General Public License      - */
/* - along with this program.  If not, see <http://www.gnu.org/licenses/>.  - */
/* -------------------------------------------------------------------------- */
/* - File:                   OxygenPreFilter.java
/* - Created by:             <https://github.com/louiscyphre>               - */
/* - Creation date and time: 22:11 27.05.2018                               - */
/* -------------------------------------------------------------------------- */
package oxygen;

import org.apache.lucene.analysis.TokenStream;

import java.util.List;

public class OxygenPreFilter {

    private static final String splitSet = "\\W";

    public static String filter(String query, List<String> stopWords) {
        return new String(query.replaceAll("(?i)"+(query.split(splitSet)[0]),"")).trim();
    }
}
