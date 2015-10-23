package org.ld4l.rdftransform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class RdfTransform {
    
    /**
     * Indicates a problem with the command-line arguments.
     */
    private static class UsageException extends Exception {
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
        
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        
        if (!inputFile.isFile()) {
            throw new UsageException("Input file '"
                    + inputFile.getAbsolutePath() + "' does not exist.");
        }
        
        if (!outputFile.getParentFile().isDirectory()) {
            throw new UsageException("Result file '"
                    + outputFile.getAbsolutePath()
                    + "' does not exist, and we can't create it "
                    + "because its parent directory doesn't exist either.");
        }
        
        // In future we would have arguments specifying which type of 
        // transformer to use.
        RdfDataTransformer transformer = 
                new SkosRbmsVocabTransformer(inputFile, outputFile);
        transformer.transform();
        
        // TODO log4j2 not working *** 
        System.out.println("Done!");
    }
        

}
