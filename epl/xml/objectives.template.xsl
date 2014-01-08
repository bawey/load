<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org/1999/xhtml">

<xsl:template match="objectives">
  <html>
  <head>
    <link rel="stylesheet" type="text/css" href="style.css" />
  </head>
  <body>
      <xsl:for-each select="objectives_group">
        <div class="wrapper">
          <div class="objectives">
            <xsl:value-of select="@name"/>
          </div>
          <xsl:for-each select="objective">
            <div class="objective {@status}">
              <xsl:if test="overview!=''">
                <div class="overview">
                  <div class="heading">overview</div>
                  <div class="content"><xsl:value-of select="overview"/></div>
                </div>
              </xsl:if>

              <xsl:if test="details!=''">
                <div class="details">
                  <div class="heading">details</div>
                  <div class="content"><xsl:value-of select="details"/></div>
                </div>
              </xsl:if>

              <xsl:if test="solution!=''">
                <div class="solution">
                  <div class="heading">solution</div>
                  <div class="content"><xsl:value-of select="solution"/></div>
                </div>
              </xsl:if>
              
              <xsl:if test="notes!=''">
                <div class="notes">
                  <div class="heading">notes</div>
                  <div class="content"><xsl:value-of select="notes"/></div>
                </div>
              </xsl:if>

              <xsl:if test="epl!=''">
                <div class="epl">
                  <div class="heading">EPL</div>
                  <div class="content">
                  <xsl:for-each select="epl">
                    <a href="../{.}"><xsl:value-of select="."/></a>
                  </xsl:for-each>
                </div>
                </div>  
              </xsl:if>
            </div>
          </xsl:for-each>
        </div>
      </xsl:for-each>
  </body>
  </html>
</xsl:template>

</xsl:stylesheet>