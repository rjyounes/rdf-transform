package org.ld4l.rdftransform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class RdfDataTransformer {

    private static final Logger LOGGER = 
            LogManager.getLogger(RdfDataTransformer.class);  
    
    private final File outfile;
    private final RDFFormat format;
    
    protected Model model;
    protected Model assertions;
    protected Model retractions;
    
    public RdfDataTransformer(File infile, File outfile, RDFFormat format) {
        this.outfile = outfile;
        this.format = format;
        
        model = ModelFactory.createDefaultModel();
        try {
            String canonicalPath = infile.getCanonicalPath();
            LOGGER.debug("Reading model from file " + canonicalPath);
            model.read(canonicalPath);            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        assertions = ModelFactory.createDefaultModel();
        retractions = ModelFactory.createDefaultModel();
    }
    


    public abstract void transform();
    
//    protected Model readModelFromFile(File file) {
//        return readModelFromFile(file.toString());
//    }
//    
//    protected Model readModelFromFile(String filename) {
//        //return RDFDataMgr.loadModel(filename);
//        LOGGER.debug("Reading file " + filename);
//        Model model = ModelFactory.createDefaultModel() ; 
//        model.read(filename);
//        return model;
//    }
    
    protected void writeNewModel() {
        applyModelChanges();
        writeModelToFile();       
    }
    
    private void applyModelChanges() {
        model.remove(retractions);
        model.add(assertions);
    }

    private void writeModelToFile() {
        
        FileOutputStream outStream;

        try {
            outStream = new FileOutputStream(outfile, false);
            RDFDataMgr.write(outStream, model, format);
                    
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }          
    }
    
}
