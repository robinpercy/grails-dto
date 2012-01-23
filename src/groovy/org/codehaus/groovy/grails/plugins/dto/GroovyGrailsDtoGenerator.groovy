package org.codehaus.groovy.grails.plugins.dto

import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
 * Created by IntelliJ IDEA.
 * User: rpercy
 * Date: 12-01-22
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
class GroovyGrailsDtoGenerator extends DefaultGrailsDtoGenerator {

    /**
     * Creates a generator of Groovy classes.
     * @param useNativeEol The generator creates files with native line
     * endings if this is <code>true</code>, otherwise it uses '\n'.
     * Default value is <code>true</code>.
     * @param indent The string to use for indenting. Defaults to 4
     * spaces.
     */
    GroovyGrailsDtoGenerator(boolean useNativeEol = true, String indent = "    ") {
        super(useNativeEol, indent)
    }

    protected Set generateNoRecurseInternal(GrailsDomainClass dc, Writer writer, String targetPkg) {
        // Deal with the persistent properties.
        def imports = []
        def fields = []
        def relations = [] as Set
        def processProperty = { prop ->
            def propType = prop.type
            def field = [ name: prop.name ]

            if (prop.referencedPropertyType == propType) {
                field["typeString"] = propType.simpleName + (prop.association ? "DTO" : "")
                addImportIfNecessary(imports, targetPkg, propType, prop.association)
            }
            else {
                field["typeString"] = propType.simpleName + '<' + prop.referencedPropertyType.simpleName + (prop.association ? "DTO" : "") + '>'
                addImportIfNecessary(imports, targetPkg, propType, false)
                addImportIfNecessary(imports, targetPkg, prop.referencedPropertyType, prop.association)
            }

            // Store the reference domain class if this property is
            // an association. This is so that we can return a set
            // of related domain classes.
            if (prop.association) relations << prop.referencedDomainClass

            fields << field
        }

        processProperty.call(dc.identifier)
        dc.properties.findAll({ it.name != dc.identifier.name }).each(processProperty)

        // Start with the package line.
        if (targetPkg) {
            writer.write "package ${targetPkg};${eol}${eol}"
        }

        // Now add any required imports.
        if (imports) {
            imports.unique().sort().each { str ->
                writer.write "import ${str};${eol}"
            }
            writer.write eol
        }

        // Next, the class declaration.
        writer.write "class ${dc.shortName}DTO implements grails.plugins.dto.DTO {${eol}"

        // A serialUID, since DTOs are serialisable.
        writer.write "${indent}private static final long serialVersionUID = 1L;${eol}${eol}"

        // The private fields.
        fields.each { field ->
            def fieldType = field.typeString == 'Object' ? 'def' : field.typeString
            writer.write "${indent}${fieldType} ${field.name};${eol}"
        }

        // Class terminator.
        writer.write "}${eol}"

        // All done. Make sure all data has been pushed to the destination
        // before we leave.
        writer.flush()

        return relations
    }

    protected File getDtoFile(File rootDir, GrailsDomainClass dc, String targetPkg) {
        def pkgPath = ""
        if (targetPkg) pkgPath = targetPkg.replace(".", "/") + '/'
        return new File(rootDir, "${pkgPath}${dc.shortName}DTO.groovy")
    }
}
