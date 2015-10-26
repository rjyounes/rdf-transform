package org.ld4l.rdftransform;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.jena.riot.RDFFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RdfTransform {
    
    /**
     * Indicates a problem with the command-line arguments.
     */
    private static class UsageException extends Exception {
        private static final long serialVersionUID = 8231538290897327025L;
        UsageException(String message) {
            super(message);
        }
    }

    private static final Logger LOGGER = 
            LogManager.getLogger(RdfTransform.class);  
    
    public RdfTransform() {
        // TODO Auto-generated constructor stub
    }

    public static void main(String[] args) throws UsageException {
        
        // Define program options
        Options options = getOptions();
        
        // Get commandline options
        CommandLine cmd = getCommandLine(options, args);
        
        if (cmd == null) {
            throw new UsageException("No commandline options specified");
        }       
        
        File infile = new File(cmd.getOptionValue("input"));
        if (!infile.isFile()) {
            throw new UsageException("Input file '"
                    + infile.getAbsolutePath() + "' does not exist.");
        }
        
        File outfile = new File(cmd.getOptionValue("output"));
        if (!outfile.getParentFile().isDirectory()) {
            throw new UsageException("Result file '"
                    + outfile.getAbsolutePath()
                    + "' does not exist, and we can't create it "
                    + "because its parent directory doesn't exist either.");
        }
        
        RDFFormat format = getRdfFormat(cmd.getOptionValue("format"));
        // TODO Check to make sure filename matches format - issue a warning
        // and change filename extension if no match.
        
        // In future we would have arguments specifying which type of 
        // transformer to use.
        RdfDataTransformer transformer = 
                new SkosRbmsVocabTransformer(infile, outfile, format);
        transformer.transform();
        
        LOGGER.info("Done!");
    }
    
    /**
     * Define the commandline options accepted by the program.
     * @return an Options object
     */
    private static Options getOptions() {
        
        Options options = new Options();

        options.addOption(Option.builder("i")
                .longOpt("input")
                .required()
                .hasArg()
                .desc("Absolute or relative path to output file.")
                .argName("input")
                .build());
             
        options.addOption(Option.builder("o")
                .longOpt("output")
                .required()
                .hasArg()
                .desc("Absolute or relative path to output file.")
                .argName("output")
                .build());

        options.addOption(Option.builder("f")
                .longOpt("format")
                .required()
                .hasArg()
                .desc("RDF serialization of output.")
                .argName("format")
                .build());

        return options;
    }

    /**
     * Parse commandline options.
     * @param options
     * @param args
     * @return
     */
    private static CommandLine getCommandLine(Options options, String[] args) {
        
        // Parse program arguments
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (MissingOptionException e) {
            LOGGER.fatal(e.getMessage());
            printHelp(options);
        } catch (UnrecognizedOptionException e) {
            LOGGER.fatal(e.getMessage());
            printHelp(options);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            LOGGER.fatal(e.getStackTrace().toString());
        }
        return null;
    }

    private static RDFFormat getRdfFormat(String format) {
        
        if (format.equals("rdfxml")) {
            return RDFFormat.RDFXML;
        } 
        if (format.equals("ntriples")) {
            return RDFFormat.NTRIPLES;
        } 
        if (format.equals("turtle")) {
            return RDFFormat.TURTLE;
        }
        // Default
        return RDFFormat.RDFXML;


    }
    /**
     * Print help text.
     * @param options
     */
    private static void printHelp(Options options) {
        
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(80);
        formatter.printHelp("rdf-transform", options, true);
    }

}
