import groovy.io.FileType
import java.security.MessageDigest

def call(sourceCodeDirectory) {
    def dir = new File(sourceCodeDirectory)
    def md5s = []
    dir.traverse(type: FileType.FILES) { file ->
        md5s << calculateMD5(file)
    }

    def finalMD5 = calculateMD5(new ByteArrayInputStream(md5s.join().getBytes()))
    print "Calcukated checksum for " + sourceCodeDirectory
    return finalMD5
}

@NonCPS
def calculateMD5(input) {
    MessageDigest md = MessageDigest.getInstance("MD5")
    if (input instanceof File) {
        input.withInputStream { is ->
            byte[] buffer = new byte[8192]
            int read
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read)
            }
        }
    } else if (input instanceof ByteArrayInputStream) {
        byte[] buffer = new byte[8192]
        int read
        while ((read = input.read(buffer)) > 0) {
            md.update(buffer, 0, read)
        }
    }
    byte[] md5sum = md.digest()
    md5sum.collect { String.format("%02x", it) }.join()
}