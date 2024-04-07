@GrabResolver(name='jitpack.io', root='https://jitpack.io/')
@Grab("com.google.errorprone:error_prone_annotations:2.20.0") // fixes alvarium import error
@Grab(group='org.slf4j', module='slf4j-api', version='2.0.12')
@Grab(group='com.github.project-alvarium', module='alvarium-sdk-java', version='7e53f04e3e') 
@Grab("org.apache.logging.log4j:log4j-core:2.21.0")

import java.util.Map;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;

import com.alvarium.SdkInfo;

import com.alvarium.contracts.AnnotationType;

import com.alvarium.annotators.Annotator;
import com.alvarium.annotators.AnnotatorConfig;
import com.alvarium.annotators.AnnotatorFactory;
import com.alvarium.annotators.ChecksumAnnotatorProps;
import com.alvarium.annotators.SourceCodeAnnotatorProps;

import com.alvarium.utils.PropertyBag;
import com.alvarium.utils.ImmutablePropertyBag;

def call(
    List<String> annotatorKinds,
    String artifactPath,
    String checksumPath,
    String sourceCodeChecksumPath,
    String sbomPath,
    SdkInfo sdkInfo,
    Logger logger
) {

    AnnotatorFactory annotatorFactory = new AnnotatorFactory();
    List<Annotator> annotators = []
    Map<String, Object> properties = new HashMap<String, Object>()

    for (annotatorKind in annotatorKinds) {
        Annotator annotator
        AnnotatorConfig cfg = getAnnotatorConfig(sdkInfo, annotatorKind)
        switch(annotatorKind) {
            case "checksum":
                File artifact = new File(artifactPath);
                File checksum = new File(
                    checksumPath
                )
                ChecksumAnnotatorProps props = new ChecksumAnnotatorProps(
                    artifactPath,
                    checksum.getAbsolutePath()
                )
                properties.put(AnnotationType.CHECKSUM.name(), props)
                annotator = annotatorFactory.getAnnotator(cfg, sdkInfo, logger)
                annotators.add(annotator)
                break;

            case "source-code":
                annotator = annotatorFactory.getAnnotator(cfg, sdkInfo, logger)
                SourceCodeAnnotatorProps props = new SourceCodeAnnotatorProps(
                    "${WORKSPACE}",
                    sourceCodeChecksumPath
                )
                properties.put(AnnotationType.SourceCode.name(), props)
                annotators.add(annotator)
                break;

            case "vulnerability":
                annotator = annotatorFactory.getAnnotator(cfg, sdkInfo, logger)
                properties.put(
                    AnnotationType.VULNERABILITY.name(), 
                    "${WORKSPACE}".toString()
                )
                annotators.add(annotator)
                break;
            
            case "sbom":
                annotator = annotatorFactory.getAnnotator(cfg, sdkInfo, logger)
                properties.put(
                    AnnotationType.SBOM.name(),
                    sbomPath
                )
                annotators.add(annotator)
                break;
        }
    }
    Annotator[] a = annotators
    PropertyBag ctx = initCtx(properties)

    return [a, ctx]
}

@NonCPS
def initCtx(properties) {
    return new ImmutablePropertyBag(properties)
}

def getAnnotatorConfig(sdkInfo, annotatorKind) {
    for (cfg in sdkInfo.getAnnotators()) {
        switch (annotatorKind) {
            case "vulnerability":
                if (cfg.getKind() == AnnotationType.VULNERABILITY) {
                    return cfg;
                }
                break;
            case "source-code":
                if (cfg.getKind() == AnnotationType.SourceCode) {
                    return cfg;
                }
                break;
            case "checksum":
                if (cfg.getKind() == AnnotationType.CHECKSUM) {
                    return cfg;
                }
                break;
            case "sbom":
                if (cfg.getKind() == AnnotationType.SBOM) {
                    return cfg;
                }
        }
    }
}
