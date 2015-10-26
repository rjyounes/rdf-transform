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
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


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


    
    // Map concept-to-concept property string values to Concept URIs
    private Map<String, String> concepts; 

    private int schemeNum;    
    private int conceptNum;

    
    public SkosRbmsVocabTransformer(File inputFile, File outputFile)  {
        super(inputFile, outputFile);
        
        loadSkosOntModel(); 
        loadOntResources();
 
        // Numbering starts from 0
        schemeNum = -1;
        conceptNum = -1;
    }
    
    private void loadSkosOntModel() {
        
        OntDocumentManager mgr = new OntDocumentManager();
        // For now, no reasoning in model
        OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);                
        spec.setDocumentManager(mgr);    
        skosOntModel = ModelFactory.createOntologyModel(spec);
        skosOntModel.read(SKOS_NS);
    }
    
    /**
     * Define OntResources needed by multiple methods.
     */
    private void loadOntResources() {

        conceptClass = skosOntModel.createClass(SKOS_NS + "Concept");
        conceptSchemeClass = 
                skosOntModel.createClass(SKOS_NS + "ConceptScheme");   
        
        prefLabelProp = 
                skosOntModel.getOntProperty(SKOS_NS + "prefLabel"); 
        
                     
    }
    
    private void loadConcepts() {

        Property notationProp = 
                skosOntModel.getOntProperty(SKOS_NS + "notation");
        
        concepts = new HashMap<String, String>();

        ResIterator subjects = model.listSubjects();
        while (subjects.hasNext()) {     
            conceptNum++;
            Resource subject = subjects.nextResource();  
            String subjectUri = subject.getURI();
            Statement stmt = subject.getProperty(prefLabelProp);
            if (stmt == null) {
                LOGGER.info("No skos:prefLabel for resource " + subjectUri);
                stmt = subject.getProperty(notationProp);
            }
            if (stmt == null) {
                LOGGER.info("No skos:notation for resource " + subjectUri);            
            } else {
                Literal literal = stmt.getLiteral();
                String label = literal.getLexicalForm();
                concepts.put(label, subjectUri);
                // Add rdfs:label
                assertions.add(subject, RDFS.label, literal);
                // LOGGER.debug(label + ": " + subjectUri);

            }
        }
    }
    
    public void transform() {
        
        loadConcepts();
        
        createConceptSchemes();
        
        transformStringToThing(skosOntModel.getOntProperty(SKOS_NS + "broader"));
                   
        transformStringToThing(skosOntModel.getOntProperty(SKOS_NS + "narrower")); 
        
        transformStringToThing(skosOntModel.getOntProperty(SKOS_NS + "related"));

        writeNewModel();
        
        LOGGER.debug("Number of concepts: " + (conceptNum + 1));
        LOGGER.debug("Number of schemes: " + (schemeNum + 1));
        
                
    }
   
    /**
     * Change:
     * :concept skos:hasTopConcept "label" .
     * 
     * to:
     * :concept skos:inScheme :conceptscheme .
     * :conceptscheme a skos:ConceptScheme ;
     * rdfs:label "label" .
     * 
     */
    private void createConceptSchemes() {

        Property hasTopConceptProp = 
                skosOntModel.getOntProperty(SKOS_NS + "hasTopConcept");
        Property inSchemeProp =
                skosOntModel.getOntProperty(SKOS_NS + "inScheme");  
        
        // Map concept-to-concept property string values to Concept URIs, to
        // determine whether to create a new concept scheme or re-use an 
        // existing one.
        Map<String, String> conceptSchemes = new HashMap<String, String>();
                  
        // Change literal values for hasTopConcept to a ConceptScheme
        // resource and an inScheme assertion.
        StmtIterator statements = 
                model.listStatements((Resource) null, hasTopConceptProp, 
                        (RDFNode) null);

        while (statements.hasNext()) {
            Statement stmt = statements.nextStatement();

            // Remove the faulty statement
            retractions.add(stmt);
            
            Resource subject = stmt.getSubject();

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

    /**
     * Change:
     * :concept1 prop "label" .
     * 
     * to:
     * :concept1 prop :concept2.
     * 
     */
    private void transformStringToThing(Property prop) {

        StmtIterator stmts = 
                model.listStatements((Resource) null, prop, (RDFNode) null); 
                                     
        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();

            // Remove the faulty statement
            retractions.add(stmt);

            Resource subject = stmt.getSubject();
            
            String conceptLabel = stmt.getLiteral().getLexicalForm();                    
            String conceptUri;
            Resource concept;
            
            // This string value has already been seen
            if (concepts.containsKey(conceptLabel)) {
                conceptUri = concepts.get(conceptLabel);
                concept = assertions.getResource(conceptUri);
                assertions.add(subject, prop, concept);
            }
            
            /*
            // New string value 
            // Commenting out because this case doesn't appear in the data
            } else {
                concept = createConcept(conceptLabel);
                concepts.put(conceptLabel, concept.getURI());
            }
            assertions.add(subject, prop, concept);
            */

        }
       
    }
    
    /*
    private Resource createConcept(String conceptLabel) { 
        
        Resource concept = assertions.createResource(mintConceptUri());
        LOGGER.debug("Created concept with URI " + concept.getURI());
        assertions.add(concept, RDF.type, conceptClass);
        assertions.add(concept, RDFS.label, conceptLabel);
        assertions.add(concept, prefLabelProp, conceptLabel);
        return concept;
    }
    
    private String mintConceptUri() {  
        conceptNum++;
        // ASK JASON: Or just use RBMS_NS? Or put concepts in RBMS_NS + 
        // "concept/" namespace too?
        return RBMS_NS + conceptNum;
    }
    */
    
    
}
