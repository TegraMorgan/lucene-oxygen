/**
 *
 */
package utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class parses and saves all command line parameters for program
 */
public class CmdParser {

  /**
   *
   */
  private CommandLine cmd;

  /**
   * @param args
   * @throws Exception
   */
  public void extract(String[] args) throws Exception {

    Options options = new Options();

    try {
      Option index = new Option("i", "index", false, "Reindex documents");
      index.setRequired(false);
      options.addOption(index);

      Option questions = new Option("q", "create-questions", false, "Create questions file from corpus json file");
        questions.setRequired(false);
      options.addOption(questions);

      CommandLineParser parser = new DefaultParser();
      cmd = parser.parse(options, args);

    } catch (ParseException e) {
      System.out.println(e.getMessage());
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java -jar yourJarName.jar", options);
      throw new Exception();
    }
  }

  public boolean hasIndexingOption() {
    return cmd.hasOption("index");
  }
  public boolean hasCreateQuiestionsOption() {
        return cmd.hasOption("create-questions");
    }
}
