<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:gal="urn:wildfly:galleon-plugins:layers-info:1.0">

    <xsl:output method="html" encoding="utf-8" standalone="no" media-type="text/html" />
    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
            </head>
            <body>
                <table class="tableblock frame-all grid-all spread">
                    <thead>
                        <tr>
                            <th class="tableblock halign-left valign-top">Name</th>
                            <th class="tableblock halign-left valign-top">Description</th>
                            <th class="tableblock halign-left valign-top">Visibility</th>
                            <th class="tableblock halign-left valign-top">Dependencies</th>
                        </tr>
                    </thead>
                    <tbody>
                        <xsl:for-each select="gal:layers-info/gal:layer[@deprecated='false']">
                            <tr>
                                <td class="tableblock halign-left valign-top"><p class="tableblock">
                                    <div>
                                        <xsl:attribute name="id">galleon.layer.<xsl:value-of select="@name"/></xsl:attribute>
                                        <xsl:value-of select="@name"/>
                                    </div>
                                </p>
                                </td>
                                <td class="tableblock halign-left valign-top"><p class="tableblock">
                                    <xsl:value-of select="gal:description" disable-output-escaping="yes"/>
                                </p>
                                </td>
                                <td class="tableblock halign-left valign-top"><p class="tableblock">
                                    <xsl:value-of select="@visibility"/>
                                </p>
                                </td>
                                <td class="tableblock halign-left valign-top"><p class="tableblock">
                                    <xsl:for-each select="gal:dependencies/gal:dependency">
                                        <xsl:choose>
                                            <xsl:when test="@optional = 'true'">
                                                <a>
                                                    <xsl:attribute name="href">#galleon.layer.<xsl:value-of select="@name"/></xsl:attribute>
                                                    <xsl:value-of select="@name"/>
                                                </a> (optional)<br/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <a>
                                                    <xsl:attribute name="href">#galleon.layer.<xsl:value-of select="@name"/></xsl:attribute>
                                                    <xsl:value-of select="@name"/>
                                                    <br/>
                                                </a>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </p>
                                </td>
                            </tr>
                        </xsl:for-each>
                    </tbody>
                </table>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
