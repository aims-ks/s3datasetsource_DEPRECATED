<?xml version="1.0" encoding="UTF-8"?>
<#--
 *  FreeMarker Catalogue template
 *    Docs: https://freemarker.apache.org/docs/
 *
 *  Copyright (C) 2019 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.gov.au>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
-->
<catalog name="eReefs model, derived Open Data Catalogue file"
         xmlns="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"
         xmlns:xlink="http://www.w3.org/1999/xlink"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0 http://www.unidata.ucar.edu/schemas/thredds/InvCatalog.1.0.6.xsd">

    <#if datasets?size != 0>

        <service name="all" base="" serviceType="compound">
            <service name="odap" serviceType="OpenDAP" base="/thredds/dodsC/" />
            <service name="http" serviceType="HTTPServer" base="/thredds/s3FileServer/" />
            <service name="wms" serviceType="WMS" base="/thredds/wms/" />
            <#--
            Those services are not yet implemented with S3 files
            <service name="dap4" serviceType="DAP4" base="/thredds/dap4/" />
            <service name="wcs" serviceType="WCS" base="/thredds/wcs/" />
            <service name="ncss" serviceType="NetcdfSubset" base="/thredds/ncss/" />
            -->
        </service>

        <#list datasets as dataset>
            <dataset name="${dataset.name}" ID="${dataset.id}" serviceName="all" urlPath="${dataset.urlPath}" dataType="Grid" />
        </#list>
    </#if>

    <#list catalogRefs as catalogRef>
        <catalogRef name="${catalogRef.name}" xlink:href="${catalogRef.href}" />
    </#list>

</catalog>
