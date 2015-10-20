import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.LinkedBlockingQueue
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

class Convert {
    def private SAXParser parser
    def private XWikiConverter xwikiConverter

    def private boolean dockerdownloaded = false

    public static void main(String[] args) {
        def c = new Convert()
        c.unzipAll(new File("xwiki").toPath())
    }

    def public Convert() {
        def instance = SAXParserFactory.newInstance()
        instance.namespaceAware = false
        parser = instance.newSAXParser();

        xwikiConverter=new XWikiConverter()


    }

    def public unzipAll(Path p) {
        p.eachFile {Path f ->
            if (f.toFile().isFile() && f.fileName.toFile().name.endsWith(".xar") ) {
//                println "found zipfile: ${f.fileName}"
<<<<<<< HEAD
                def zip = new ZipFile(f.toFile())
                def List<ZipEntry> found = zip.entries().findAll {entry ->
                    entry.name.endsWith(".xml") && !entry.name.endsWith("package.xml")// && entry.name.contains("05 Security")
                }

                found.each {it ->
                    def fileName = it.name
                    println "parsing ${fileName}"

                    def outPath = new File(fileName).toPath()
                    def targetDir = Paths.get("build/markdown", outPath.parent.toFile().name)
                    targetDir.toFile().mkdirs()
                    def outFile = targetDir.resolve(outPath.subpath(1, outPath.nameCount))
                    def newName = outFile.fileName.toString().replace(".xml", ".md")
                    def mdOutputFile = outFile.resolveSibling(newName);


                    def content = readXWikiContent(zip.getInputStream(it), targetDir)
                    // write attachments
                    content.attachments.each {it.writeToPath(targetDir)}

                    def processedString = content.stringContent
                    processedString = preprocessXwikiString(content.stringContent)
                    processedString = xwikiConverter.convert(processedString)
                    processedString = convertHTMLStringToMarkdownString(processedString)
                    processedString = postProcessMarkdownString(processedString, mdReplacements)

//                    def all = processedString.replaceAll("[ ]", "?")
//                    all.each {println "unprintable: ${it}"}

                    println "writing converted markdown to ${mdOutputFile}"
                    mdOutputFile.write(processedString)
=======
                try {
                    def zip = new ZipFile(f.toFile())
                    def List<ZipEntry> found = zip.entries().findAll {entry ->
                        entry.name.endsWith(".xml") && !entry.name.endsWith("package.xml") //&& entry.name.contains("01 Introduction")
                    }

                    found.each {it ->
                        println "parsing ${it.name}"
                        convertXwikiToMarkdown(zip.getInputStream(it), it.name)
                    }
                } catch (ZipException e ) {
                    println "Error opening: $f"
                    e.printStrackTrace()
>>>>>>> 887f528d0d8845ebbb545c1a95d5cc2ccb83d7cc
                }
            }
        }
    }

    def public XWikiContent readXWikiContent(InputStream is, Path targetDir) {

        def result = new XWikiContent()

        def StringBuilder stringContent = new StringBuilder()
        def Attachment attachment
        def String attachmentFilename;


        parser.parse(is, new DefaultHandler() {
            def boolean contentParsing = false;
            def boolean attachmentParsing = false;
            def boolean filenameParsing = false;

            @Override
            void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                super.startElement(uri, localName, qName, attributes)
//                println "StartElement ${qName}, ${attributes}"
                if (qName.equals("content")) {
                    contentParsing =true
//                    println "startContent"
                } else if (qName.equals("attachment")) {
                    attachmentParsing = true;
//                    println "startAttachment"
                    attachment = new Attachment()
                    result.attachments.add(attachment)
                } else if (qName.equals("filename")) {
                    filenameParsing = true;
                    attachmentFilename = new String()
                }

            }

            @Override
            void endElement(String uri, String localName, String qName) throws SAXException {
                super.endElement(uri, localName, qName)
                if (qName.equals("content")) {
                    contentParsing = false
//                    println "stopContent"
                    if (attachmentParsing) {

                    } else {
                        // end
                        result.stringContent = stringContent.toString()
                    }
                } else if (qName.equals("attachment")) {
                    attachmentParsing = false
//                    println "stopAttachment"
                } else if (qName.equals("filename")) {
                    filenameParsing = false;
                    attachment.fileName = attachmentFilename
                }
            }

            @Override
            void characters(char[] ch, int start, int length) throws SAXException {
                super.characters(ch, start, length)
                if (attachmentParsing)  {
                    if (filenameParsing) {
                        attachmentFilename += new String(ch, start, length)
                    } else if (contentParsing) {
                        attachment.attachmentContent.append new String(ch, start, length)
                    }
                } else {
                    if (contentParsing) {
                        def s = new String(ch, start, length)
                        def oldString = stringContent.toString();
                        stringContent.append s
                    }
                }

            }
        })

        return result
    }

    String convertHTMLStringToMarkdownString(String htmlString) {

<<<<<<< HEAD
        def builder = new ProcessBuilder('/usr/bin/pandoc', '-f', 'html', '-t', 'markdown', '--no-wrap');
        builder.redirectErrorStream(true)
=======
        def htmlString = xwikiConverter.convert(s)

        def builder = getPandocProcessBuilder()

//        builder.redirectErrorStream(true)
>>>>>>> 887f528d0d8845ebbb545c1a95d5cc2ccb83d7cc
        def p = builder.start();

        def bytes = new ByteArrayOutputStream()
        def readerThread = new StreamGobbler(p.inputStream, "", bytes);
        readerThread.start()
        def errorReaderThread = new StreamGobbler(p.errorStream, "", null, true);
        errorReaderThread.start()

        def outWriter = p.out.newWriter()
        outWriter.write(htmlString)
        outWriter.flush()
        p.out.close()

        readerThread.join()

        p.waitFor()

        if (p.exitValue() != 0) {
            println bytes.toString()
            System.exit(p.exitValue())
        }

        return bytes.toString()
    }

<<<<<<< HEAD
    def int macroCounter
    def Queue<String[]> macroContent

    String preprocessXwikiString(String s) {
        macroCounter = 0;
        macroContent = new LinkedBlockingQueue<String[]>()
=======
    def ProcessBuilder getPandocProcessBuilder() {
        def ProcessBuilder result;
        def pandoc = "which pandoc".execute().text.trim()
        def docker = "which docker".execute().text.trim()
        if (pandoc != null && !pandoc.empty) {
            result = new ProcessBuilder(pandoc, '-f', 'html', '-t', 'markdown', '--no-wrap');
        } else if (docker != null && !docker.empty) {
            if (!dockerdownloaded) {
                println "Downloading pandoc docker image..."
                def builder = new ProcessBuilder(docker, 'pull', 'jagregory/pandoc:latest')
                builder.redirectErrorStream(true)
                def p = builder.start()
                def gobbler = new StreamGobbler(p.inputStream, "", null, true)
                gobbler.start()
                gobbler.join()
                p.out.close()
                p.waitFor()
                if (p.exitValue() != 0) {
                    System.exit(p.exitValue())
                }
                dockerdownloaded = true
            }

            result = new ProcessBuilder(docker, 'run', '-i', '-a', 'stdin', '-a', 'stdout', 'jagregory/pandoc', '-f', 'html', '-t', 'markdown', '--no-wrap');
        }

        return result;
    }

    String preprocessXwiki(String s) {
>>>>>>> 887f528d0d8845ebbb545c1a95d5cc2ccb83d7cc

        def matcher = preprocessReplacements.codeMacro.pattern.matcher(s);
        def sb = new StringBuffer()
        while (matcher.find()) {
            def String lang = null
            def String content = null
            if (matcher.groupCount() == 2) {
                content = matcher.group(2)
                lang = matcher.group(1)
            } else {
                content = matcher.group(1)
            }
            macroContent.add([lang, content] as String[])
            def String replacement = "XXXCODEBLOCKXXX"
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            macroCounter++;
        }
        matcher.appendTail(sb)
        return applyReplacements(sb.toString(), preprocessReplacements)
    }

    String postProcessMarkdownString(String s, replacements) {
        def pattern = ~/XXXCODEBLOCKXXX/
        def matcher = pattern.matcher(s)

        def sb = new StringBuffer()
        while (matcher.find()) {
            def codeBlock = macroContent.remove()
            def String lang = codeBlock[0]? codeBlock[0] : ""
            def String replacement = "\n```${lang}\n${codeBlock[1]}\n```\n"
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            macroCounter++;
        }
        matcher.appendTail(sb)

        return applyReplacements(sb.toString(), replacements)
    }


    def preprocessReplacements = [
            codeMacro: [ // {{code language="java"}} .... {{/code}}
                    pattern: ~/\{\{code(?: language="(\w*)")?\}\}([\W\w]*?)\{\{\\/code\}\}/,
                    replacement: 'XXXBACKTICKSXXX$1\n$2\nXXXBACKTICKSXXX',
            ],
            macroWorkaround: [
                pattern: ~/\{\{(\w*) ([^\}]*)\}\}([\w\W]*)\{\{\\/\1\}\}/,
                replacement: 'BEGIN MACRO: $1 param: $2 \n $3\n END MACRO: $1'
            ],
            singleTagMacroWorkaround: [ // {{toc start="2" depth="2"/}}
                    pattern: ~/\{\{(\w*) ([^\}]*)\\/\}\}/,
                    replacement: 'BEGIN MACRO: $1 param: $2 END MACRO: $1'
            ],
            newlines: [
                    pattern: Pattern.compile("\\\\\\\\"),
                    replacement: '\n'
            ],
    ]

    def mdReplacements = [
            img: [
                    pattern: ~/!\[([^\]]*@.*.\w*)\]\(([^\]]*)@(\w*.\w*)\)/,
                    replacement: '!\\[$1\\]\\($3\\)'
                    ],
            headlinks: [
                    pattern: ~/\{#.*\}/,
                    replacement: ''
                    ],
//            newlines: [
//                    pattern: Pattern.compile("\\\\\\\\"),
//                    replacement: '\n'
//            ],
            linksRelative: [
                    pattern: ~/([^!])\[([^\]]*)\]\((?:doc:){0}([^h][^t][^t][^p][\S]*)\.([\S]*)\)/,
                    replacement: '$1[$2]($3/$4)'
            ],
            linksAbsolute: [
                    pattern: ~/([^!])\[([^\]]*)\]\((?:doc:)([^h][^t][^t][^p][\S]*)\.([\S]*)\)/,
                    replacement: '$1[$2](/$3/$4)'
            ],
            linkWithoutSubdirRel: [
                    pattern: ~/([^!])\[([^\]]*)\]\((?:doc:){0}([^h][^t][^t][^p][^)]*)\)/,
                    replacement: '$1[$2]($3)'
            ],
            linkWithoutSubdirAbs: [
                    pattern: ~/([^!])\[([^\]]*)\]\((?:doc:)([^h][^t][^t][^p][^)]*)\)/,
                    replacement: '$1[$2](/$3)'
            ],
//            backticks: [
//                    pattern: ~/XXXBACKTICKSXXX/,
//                    replacement: '```'
//            ]
            anchors: [
                    pattern: ~/\{#[^\}]+\}/,
                    replacement: ''
            ]



    ]


    String applyReplacements(String s, replacements) {

        replacements.each {rep ->
            def matcher = rep.value.pattern.matcher(s)
            s = matcher.replaceAll(rep.value.replacement)
        }

        return s;
    }
}

