import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.Paths

class HashProvider {
    MessageDigest md

    HashProvider() {
        md = MessageDigest.getInstance("MD5")
    }

    @NonCPS
    void update(byte[] buffer, int offset, int length) {
        md.update(buffer, offset, length)
    }

    @NonCPS
    void update(byte[] buffer) {
        md.update(buffer)
    }

    @NonCPS
    String getValue() {
        byte[] digest = md.digest()
        digest.collect { String.format("%02x", it) }.join()
    }

    @NonCPS
    String derive(byte[] input) {
        md.update(input)
        getValue()
    }
}

@NonCPS
def getAllFiles(String path) {
    def files = []
    def directory = new File(path)

    if (directory.isDirectory()) {
        def directoryFiles = directory.listFiles()
        if (directoryFiles != null) {
            directoryFiles.each { file ->
                if (file.isFile()) {
                    files << file.getAbsolutePath()
                } else if (file.isDirectory()) {
                    files += getAllFiles(file.getAbsolutePath())
                }
            }
        }
    } else if (directory.isFile()) {
        files << directory.getAbsolutePath()
    }
    files
}

@NonCPS
def readAndHashFile(String filePath, HashProvider hashProvider) {
    def fs = new FileInputStream(filePath)
    def buffer = new byte[8192]
    def bytesRead = 0
    while (true) {
        bytesRead = fs.read(buffer)
        if (bytesRead == -1) { // indicates EOF
            break
        } else {
            hashProvider.update(buffer, 0, bytesRead)
        }
    }
    fs.close()
    hashProvider.getValue()
}

@NonCPS
def generateChecksum(String path, HashProvider hashProvider) {
    def filePaths = getAllFiles(path)
    for (int i = 0; i < filePaths.size(); i++) {
        def hashThenPath = readAndHashFile(filePaths[i], hashProvider) + "  " + filePaths[i]
        filePaths[i] = hashThenPath
    }
    filePaths.sort()

    def hashesAndFiles = filePaths.join("\n") + "\n"
    def sourceCodeChecksum = hashProvider.derive(hashesAndFiles.getBytes())

    sourceCodeChecksum
}

def call(sourceCodeDir) {
    def hashProvider = new HashProvider()
    generateChecksum(sourceCodeDir, hashProvider)
}