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
/* - File:                   QuestionAnswered.java
/* - Created by:             <https://github.com/louiscyphre>               - */
/* - Creation date and time: 20:49 01.06.2018                               - */
/* -------------------------------------------------------------------------- */
package oxygen;

import java.util.ArrayList;
import java.util.List;

public class QuestionAnswered {
    private Long id;
    private List<Answer> answers;

    QuestionAnswered(Long questionId, List<Answer> answersArray) {
        id = new Long(questionId);
        answers = new ArrayList<>();
        answers.addAll(answersArray);
    }

    @Override
    public String toString() {
        return new String(id.toString() + answers.toString());
    }
}
