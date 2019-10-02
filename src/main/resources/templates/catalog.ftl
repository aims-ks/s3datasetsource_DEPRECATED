<?xml version="1.0" encoding="UTF-8"?>
<catalog name="eReefs model, derived Open Data Catalogue file"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0 http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.6.xsd">

    <#if datasets?size != 0>

        <service name="all" base="" serviceType="compound">
            <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />
            <service name="dap4" serviceType="DAP4" base="/thredds/dap4/" />
            <service name="http" serviceType="HTTPServer" base="/thredds/s3FileServer/" />
            <service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
            <service name="wms" serviceType="WMS" base="/thredds/wms/" />
            <!--<service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/" />-->
        </service>

        <#list datasets as dataset>
            <dataset name="${dataset.name}" ID="${dataset.id}" serviceName="all" urlPath="${dataset.urlPath}" dataType="Grid" />
        </#list>
    </#if>

    <#list catalogRefs as catalogRef>
        <catalogRef xlink:title="${catalogRef.name}" xlink:href="${catalogRef.href}" name=""/>
    </#list>

</catalog>
