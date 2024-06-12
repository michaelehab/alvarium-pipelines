@GrabResolver(name='jitpack.io', root='https://jitpack.io/')
@Grab("com.google.errorprone:error_prone_annotations:2.20.0") // fixes alvarium import error
@Grab(group='com.github.michaelehab', module='alvarium-sdk-java', version='c981582bf6') 
@Grab("org.apache.logging.log4j:log4j-core:2.23.1")

import com.alvarium.annotators.sourcecode.CheckSumCalculator;
import com.alvarium.hash.HashType;

def call(sourceCodeDir) {
    CheckSumCalculator checkSumCalculator = new CheckSumCalculator(HashType.MD5Hash)
    print "Calculating source-code checksum for " + sourceCodeDir
    checkSumCalculator.generateChecksum(sourceCodeDir)
}