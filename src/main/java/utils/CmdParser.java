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
   * @throws ParseException
   */
  public void extract(String[] args) throws ParseException {

    Options options = new Options();

    Option address = new Option("i", "index", false, "Reindex documents");
    address.setRequired(false);
    options.addOption(address);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("java -jar yourJarName.jar", options);
      throw e;
    }
  }


  public boolean hasIndexingOption() {
    /*
    String option = cmd.getOptionValue("index");
    return option != null;
    */
    return cmd.hasOption("index");
  }

}
