package mdt.sample;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.AASXDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.aasx.InMemoryFile;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.xml.XmlDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class ParseAasxExample {
    private record RecoveryResult(Environment environment, int insertedKeyValueCount) { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java ParseAasxExample <file.aasx>");
            System.exit(1);
        }

        String aasxPath = args[0];

        /*
         * 매우 큰 AASX 안에 PDF, 이미지 등 압축률이 높은 첨부 파일이 들어 있으면
         * Apache POI의 zip bomb 보호 로직이 예외를 발생시킬 수 있습니다.
         *
         * 신뢰할 수 있는 파일에 한해서만 완화하세요.
         * 완전히 끄려면 0.0, 일부 보호를 유지하려면 0.001 정도를 고려할 수 있습니다.
         */
        ZipSecureFile.setMinInflateRatio(0.001);

        try (InputStream inputStream = new FileInputStream(aasxPath)) {
            AASXDeserializer deserializer = new AASXDeserializer(inputStream);

            // AASX 내부의 aas-spec XML 또는 JSON을 읽어 Environment로 역직렬화
            Environment env;
            try {
                env = deserializer.read();
            }
            catch (DeserializationException e) {
                RecoveryResult recovered = tryRecoverFromMissingKeyValue(deserializer, e);
                if (recovered == null) {
                    System.err.println("AAS model deserialization failed: " + e.getMessage());
                    throw e;
                }

                env = recovered.environment();
                System.err.printf(
                    "AAS XML deserialization recovered by inserting empty <value> into %d key element(s).%n",
                    recovered.insertedKeyValueCount()
                );
                System.err.println(
                    "The AASX seems to contain one or more Reference/Key entries without a value. "
                    + "Please validate and fix the source model if possible."
                );
            }

            printEnvironment(env);

            // AASX 내부의 supplementary file, thumbnail 등 관련 파일 읽기
            List<InMemoryFile> relatedFiles = deserializer.getRelatedFiles();
            System.out.println();
            System.out.println("Related files: " + relatedFiles.size());

            for (InMemoryFile file : relatedFiles) {
                System.out.printf(
                    "- path=%s, size=%d bytes%n",
                    file.getPath(),
                    file.getFileContent().length
                );
            }
        } catch (InvalidFormatException e) {
            System.err.println("Invalid AASX package format: " + e.getMessage());
            throw e;
        }
    }

    private static RecoveryResult tryRecoverFromMissingKeyValue(AASXDeserializer deserializer,
                                                                DeserializationException cause) {
        if (!isMissingKeyValueProblem(cause)) {
            return null;
        }

        try {
            String resource = deserializer.getResourceString();
            if (!looksLikeXml(resource)) {
                return null;
            }

            SanitizedXml sanitized = insertMissingKeyValues(resource);
            if (sanitized.insertedKeyValueCount() <= 0) {
                return null;
            }

            Environment env = new XmlDeserializer().read(sanitized.xml());
            return new RecoveryResult(env, sanitized.insertedKeyValueCount());
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isMissingKeyValueProblem(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains("valueNode") && message.contains("null")) {
                return true;
            }

            for (StackTraceElement element : cause.getStackTrace()) {
                if ("org.eclipse.digitaltwin.aas4j.v3.dataformat.xml.internal.deserialization.KeyDeserializer"
                        .equals(element.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean looksLikeXml(String content) {
        return content != null && content.stripLeading().startsWith("<");
    }

    private record SanitizedXml(String xml, int insertedKeyValueCount) { }

    private static SanitizedXml insertMissingKeyValues(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        disableExternalEntities(factory);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));

        NodeList keyNodes = document.getElementsByTagNameNS("*", "key");
        int inserted = 0;
        for (int i = 0; i < keyNodes.getLength(); ++i) {
            Element keyElement = (Element) keyNodes.item(i);
            if (findChildElement(keyElement, "value") == null) {
                keyElement.appendChild(createChildElement(document, keyElement, "value"));
                ++inserted;
            }
        }

        return new SanitizedXml(toXmlString(document), inserted);
    }

    private static void disableExternalEntities(DocumentBuilderFactory factory) {
        setFeatureQuietly(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setFeatureQuietly(factory, "http://xml.org/sax/features/external-general-entities", false);
        setFeatureQuietly(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeatureQuietly(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }

    private static void setFeatureQuietly(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        }
        catch (Exception ignored) {
            // ignore parser-specific unsupported features
        }
    }

    private static Element findChildElement(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String childLocalName = child.getLocalName();
                String childNodeName = child.getNodeName();
                if (localName.equals(childLocalName) || localName.equals(childNodeName)) {
                    return (Element) child;
                }
            }
        }
        return null;
    }

    private static Element createChildElement(Document document, Element parent, String localName) {
        String namespaceUri = parent.getNamespaceURI();
        String prefix = parent.getPrefix();
        String qualifiedName = (prefix == null || prefix.isBlank()) ? localName : prefix + ":" + localName;
        return namespaceUri != null
                ? document.createElementNS(namespaceUri, qualifiedName)
                : document.createElement(localName);
    }

    private static String toXmlString(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private static void printEnvironment(Environment env) {
        System.out.println("AAS count: " + sizeOf(env.getAssetAdministrationShells()));
        System.out.println("Submodel count: " + sizeOf(env.getSubmodels()));
        System.out.println("ConceptDescription count: " + sizeOf(env.getConceptDescriptions()));
        System.out.println();

        if (env.getAssetAdministrationShells() != null) {
            System.out.println("[AssetAdministrationShells]");
            for (AssetAdministrationShell aas : env.getAssetAdministrationShells()) {
                System.out.printf(
                    "- idShort=%s, id=%s%n",
                    aas.getIdShort(),
                    aas.getId()
                );
            }
        }

        if (env.getSubmodels() != null) {
            System.out.println();
            System.out.println("[Submodels]");
            for (Submodel submodel : env.getSubmodels()) {
                System.out.printf(
                    "- idShort=%s, id=%s, elements=%d%n",
                    submodel.getIdShort(),
                    submodel.getId(),
                    sizeOf(submodel.getSubmodelElements())
                );
            }
        }

        if (env.getConceptDescriptions() != null) {
            System.out.println();
            System.out.println("[ConceptDescriptions]");
            for (ConceptDescription cd : env.getConceptDescriptions()) {
                System.out.printf(
                    "- idShort=%s, id=%s%n",
                    cd.getIdShort(),
                    cd.getId()
                );
            }
        }
    }

    private static int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }
}