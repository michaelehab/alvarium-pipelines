@GrabResolver(name='jitpack.io', root='https://jitpack.io/')
@Grab("com.google.errorprone:error_prone_annotations:2.20.0") // fixes alvarium import error
@Grab(group='com.github.project-alvarium', module='alvarium-sdk-java', version='d18f5aeadd') 
@Grab("org.apache.logging.log4j:log4j-core:2.15.0")

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alvarium.DefaultSdk;
import com.alvarium.Sdk;
import com.alvarium.SdkInfo;

import com.alvarium.annotators.Annotator;
import com.alvarium.utils.PropertyBag;

def call(List<String> annotatorKinds, String artifactPath=null) {

    if (annotatorKinds.contains('checksum') && artifactPath == null) {
        error "Checksum annotator requires the `artifactPath` parameter"
    }

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

    def (Annotator[] annotators, PropertyBag ctx) = alvariumGetAnnotators(
        annotatorKinds,
        artifactPath,
        sdkInfo,
        logger    
    )

    DefaultSdk sdk = new DefaultSdk(annotators, sdkInfo, logger)
    sdk.transit(ctx, pipelineId.getBytes())
    sdk.close()
}

@NonCPS 
def getSdkInfoFromJson(String json) {
    def info = SdkInfo.fromJson(json)
    return info
}
