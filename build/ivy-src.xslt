<xsl:stylesheet version = "1.0"
  xmlns:xsl = "http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan = "http://xml.apache.org/xalan">

  <xsl:output method = "xml" encoding = "UTF-8"
    indent = "yes" xalan:indent-amount = "2"/>
  
  <xsl:param name = "module"/>

  <xsl:template match = "/">
    <ivy-module version = "2.0">
      <info organisation = "WARLord Project">
        <xsl:attribute name = "module">
          <xsl:value-of select = "$module"/>
        </xsl:attribute>
      </info>
      <dependencies defaultconfmapping = "* -&gt; sources">
        <xsl:apply-templates select = "modules/module"/>
      </dependencies>
    </ivy-module>
  </xsl:template>

  <xsl:template match = "module">
    <dependency org = "{@organisation}" name = "{@name}" rev = "{@rev}"/>
  </xsl:template>
</xsl:stylesheet>