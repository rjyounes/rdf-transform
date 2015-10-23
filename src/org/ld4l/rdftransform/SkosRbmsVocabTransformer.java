package org.ld4l.rdftransform;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class SkosRbmsVocabTransformer extends RdfDataTransformer {
    
    private static final Logger LOGGER = 
                LogManager.getLogger(SkosRbmsVocabTransformer.class);  
    
    private static final String SKOS_NS = 
            "http://www.w3.org/2004/02/skos/core#";
    
    private static final String RBMS_NS = "http://rbms.info/vocab/";  
    private static final String RBMS_SCHEME_NS = RBMS_NS + "scheme/";
    
    private OntModel skosOntModel;
    private OntClass conceptClass;
    private OntClass conceptSchemeClass;
    private Property prefLabelProp;
    private Property notationProp;
    private Property hasTopConceptProp;
    private Property inSchemeProp;
    private Property broaderProp;
    private Property narrowerProp;
    private Property relatedProp;
    
    private Map<String, String> conceptSchemes;
    Map<String, String> concepts; 
    
    // Numbering starts from 0
    private static int schemeNum = -1;    
    private static int conceptNum = -1;

    public SkosRbmsVocabTransformer(File inputFile, File outputFile)  {
        super(inputFile, outputFile);
        
        loadSkosOntModel(); 
        loadOntResources();
        
        // Map hasTopConcept string values to ConceptScheme URIs
        conceptSchemes = new HashMap<String, String>();
        
        loadConcepts();

    }
    
    private void loadSkosOntModel() {
        
        OntDocumentManager mgr = new OntDocumentManager();
        // For now, no reasoning in model
        OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);                
        spec.setDocumentManager(mgr);    
        skosOntModel = ModelFactory.createOntologyModel(spec);
        skosOntModel.read(SKOS_NS);
    }
    
    private void loadOntResources() {

        conceptClass = skosOntModel.createClass(SKOS_NS + "Concept");
        conceptSchemeClass = 
                skosOntModel.createClass(SKOS_NS + "ConceptScheme");

        prefLabelProp = skosOntModel.getOntProperty(SKOS_NS + "prefLabel"); 
        notationProp = skosOntModel.getOntProperty(SKOS_NS + "notation");
        hasTopConceptProp = 
                skosOntModel.getOntProperty(SKOS_NS + "hasTopConcept");
        inSchemeProp = skosOntModel.getOntProperty(SKOS_NS + "inScheme");                
        broaderProp = skosOntModel.getOntProperty(SKOS_NS + "broader");                
        narrowerProp = skosOntModel.getOntProperty(SKOS_NS + "narrower");                
        relatedProp = skosOntModel.getOntProperty(SKOS_NS + "related");                       
    }
    
    private void loadConcepts() {
        
        // Map concept-to-concept property string values to Concept URIs
        concepts = new HashMap<String, String>();   
        
        ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {     
            conceptNum++;
            Resource subject = subjects.nextResource();         
            String uri = subject.getURI();
            Statement stmt = subject.getProperty(prefLabelProp);
            if (stmt == null) {
                //System.out.println("No skos:prefLabel for resource " + uri);
                stmt = subject.getProperty(notationProp);
            }
            if (stmt != null) {
                Literal literal = stmt.getLiteral();
                String label = literal.getLexicalForm();
                concepts.put(label, subject.getURI());
                // Add rdfs:label
                assertions.add(subject, RDFS.label, literal);
                //System.out.println(label + ": " + uri);
            } else {
                //System.out.println("No skos:notation for resource " + uri);
            }

        }
    }
    
    public void transform() {

        ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {
            
            Resource subject = subjects.nextResource(); 
            
            // Assert that every resource is a skos:Concept
            assertions.add(subject, RDF.type, conceptClass);
            
            // TODO Combine methods for transforming hasTopConcept and other 
            // datatype props - they're mostly the same.
            
            transformHasTopConcept(subject);
            
            transformDatatypePropToObjectProp(subject, broaderProp);
            transformDatatypePropToObjectProp(subject, narrowerProp);
            transformDatatypePropToObjectProp(subject, relatedProp);
        }

        writeNewModel();
        
        System.out.println("Number of concepts: " + (conceptNum + 1));
        System.out.println("Number of schemes: " + (schemeNum + 1));
        
//        for (Map.Entry<String, String> entry : concepts.entrySet()) {
//            System.out.println(entry.getKey() + ": " + entry.getValue());
//        }
                
    }
   
    private void transformHasTopConcept(Resource subject) {
        
        // Change literal values for hasTopConcept to a ConceptScheme
        // resource and an inScheme assertion.
        StmtIterator hasTopConceptStmts = 
                subject.listProperties(hasTopConceptProp);
        while (hasTopConceptStmts.hasNext()) {
            Statement stmt = hasTopConceptStmts.nextStatement();

            // Remove the faulty statement
            retractions.add(stmt);
            String schemeLabel = stmt.getLiteral().getLexicalForm();                    
            String schemeUri;
            Resource scheme;
            
            // This string value has already been seen
            if (conceptSchemes.containsKey(schemeLabel)) {
                schemeUri = conceptSchemes.get(schemeLabel);
                scheme = assertions.getResource(schemeUri);
            // New string value  
            } else {
                scheme = createScheme(schemeLabel);
                conceptSchemes.put(schemeLabel, scheme.getURI());
            }
            assertions.add(subject, inSchemeProp, scheme);

        }
    }
    
    private Resource createScheme(String schemeLabel) { 
           
        Resource scheme = assertions.createResource(mintSchemeUri());
        assertions.add(scheme, RDF.type, conceptSchemeClass);
        assertions.add(scheme, RDFS.label, schemeLabel);
        assertions.add(scheme, prefLabelProp, schemeLabel);
        return scheme;
    }
    
    private String mintSchemeUri() {      
        schemeNum++;
        // ASK JASON: Or just use RBMS_NS? Or put concepts in RBMS_NS + 
        // "concept/" namespace too?
        return RBMS_SCHEME_NS + schemeNum;
    }
    
    private void transformDatatypePropToObjectProp(Resource subject, 
            Property prop) {

        StmtIterator stmts = 
                subject.listProperties(prop);
        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();

            // Remove the faulty statement
            retractions.add(stmt);
            String conceptLabel = stmt.getLiteral().getLexicalForm();                    
            String conceptUri;
            Resource concept;
            
            // This string value has already been seen
            if (concepts.containsKey(conceptLabel)) {
                conceptUri = concepts.get(conceptLabel);
                concept = assertions.getResource(conceptUri);
            // New string value 
            // NB This case doesn't appear in the data
            } else {
                concept = createConcept(conceptLabel);
                concepts.put(conceptLabel, concept.getURI());
            }
            assertions.add(subject, prop, concept);

        }
       
    }
    
    private Resource createConcept(String conceptLabel) { 
        
        Resource concept = assertions.createResource(mintConceptUri());
        System.out.println("Created concept with URI " + concept.getURI());
        assertions.add(concept, RDF.type, conceptClass);
        assertions.add(concept, RDFS.label, conceptLabel);
        assertions.add(concept, prefLabelProp, conceptLabel);
        return concept;
    }
    
    private String mintConceptUri() {  
        conceptNum++;
        // ASK JASON: Or just use RBMS_NS? Or put concepts in RBMS_NS + 
        // "concept/" namespace too?
        return RBMS_NS + (conceptNum);
    }
    
    
}
