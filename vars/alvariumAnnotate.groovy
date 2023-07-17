@GrabResolver(name='jitpack.io', root='https://jitpack.io/')
@Grab("com.google.errorprone:error_prone_annotations:2.20.0") // fixes alvarium import error
@Grab(group='com.github.project-alvarium', module='alvarium-sdk-java', version='d18f5aeadd') 
@Grab("org.apache.logging.log4j:log4j-core:2.15.0")

import java.util.Map;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alvarium.DefaultSdk;
import com.alvarium.Sdk;
import com.alvarium.SdkInfo;

import com.alvarium.contracts.AnnotationType;

import com.alvarium.annotators.Annotator;
import com.alvarium.annotators.AnnotatorConfig;
import com.alvarium.annotators.AnnotatorFactory;
import com.alvarium.annotators.ChecksumAnnotator;
import com.alvarium.annotators.ChecksumAnnotatorProps;
import com.alvarium.annotators.SourceCodeAnnotator;
import com.alvarium.annotators.SourceCodeAnnotatorProps;

import com.alvarium.utils.PropertyBag;
import com.alvarium.utils.ImmutablePropertyBag;

def call(List<String> annotatorKinds, String artifactPath=null) {
    Logger logger = LogManager.getRootLogger()

    String pipelineId = "${JOB_NAME}/${BUILD_NUMBER}".toString()
    String jsonString

    // Loading SDK configuration requires Jenkins' Config File Provider plugin
    // and a populated SDK configuration file with id `alvarium-config`
    configFileProvider(
        [configFile(fileId: 'alvarium-config', variable: 'SDK_INFO')]) {
        jsonString = new File("$SDK_INFO").text
    }

    SdkInfo sdkInfo = getSdkInfoFromJson(jsonString)

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
                    "${JENKINS_HOME}/jobs/${JOB_NAME}/${BUILD_NUMBER}/${artifact.getName()}.checksum"
                )
                ChecksumAnnotatorProps props = new ChecksumAnnotatorProps(
                    artifactPath,
                    checksum.getAbsolutePath()
                )

                annotator = annotatorFactory.getAnnotator(cfg, sdkInfo, logger)
                final Annotator[] a = [annotator]
                DefaultSdk sdk = new DefaultSdk(a, sdkInfo, logger)
                PropertyBag ctx = new ImmutablePropertyBag(
                    Map.of(AnnotationType.CHECKSUM.name(), props)
                )

                checksumValue = checksum.text
                sdk.mutate(ctx, pipelineId.getBytes(), checksumValue.getBytes())
                sdk.close()
                break;

            case "source-code":
                annotator = annotatorFactory.getAnnotator(cfg, sdkInfo, logger)
                SourceCodeAnnotatorProps props = new SourceCodeAnnotatorProps(
                    "${WORKSPACE}",
                    "${JENKINS_HOME}/${JOB_NAME}/${BUILD_NUMBER}/checksum"
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
        }
    }
    final Annotator[] a = annotators
    PropertyBag ctx = initCtx(properties)
    DefaultSdk sdk = new DefaultSdk(a, sdkInfo, logger)
    sdk.create(ctx, pipelineId.getBytes())
    sdk.close()

}

//see: https://stackoverflow.com/questions/50855961/notserializableexception-in-jenkinsfile
// for why @NonCPS is used. This won't compile except in a Jenkins environment
@NonCPS 
def getSdkInfoFromJson(String json) {
    def info = SdkInfo.fromJson(json)
    return info
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
        }
    }
}
