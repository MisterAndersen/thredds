/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.ncss.view.dsg.point;

import org.springframework.http.HttpHeaders;
import thredds.server.ncss.exception.NcssException;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.util.Format;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cwardgar on 2014/06/03.
 */
public class PointSubsetWriterXML extends AbstractPointSubsetWriter {
    private final XMLStreamWriter staxWriter;

    public PointSubsetWriterXML(FeatureDatasetPoint fdPoint, SubsetParams ncssParams, OutputStream out)
            throws XMLStreamException, NcssException, IOException {
        super(fdPoint, ncssParams);

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        staxWriter = factory.createXMLStreamWriter(out, "UTF-8");
    }

    @Override
    public HttpHeaders getHttpHeaders(String datasetPath, boolean isStream) {
        HttpHeaders httpHeaders = new HttpHeaders();

        if (!isStream) {
            httpHeaders.set("Content-Location", datasetPath);
            String fileName = TdsPathUtils.getFileNameForResponse(datasetPath, ".xml");
            httpHeaders.set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }

        httpHeaders.set(ContentType.HEADER, ContentType.xml.getContentHeader());
        return httpHeaders;
    }

    @Override
    public void writeHeader(PointFeature pf) throws XMLStreamException {
        staxWriter.writeStartDocument("UTF-8", "1.0");
        staxWriter.writeCharacters("\n");
        staxWriter.writeStartElement("pointFeatureCollection");
    }

    @Override
    public void writePoint(PointFeature pointFeat) throws XMLStreamException, IOException {
        EarthLocation loc = pointFeat.getLocation();

        staxWriter.writeCharacters("\n    ");
        staxWriter.writeStartElement("pointFeature");
        staxWriter.writeAttribute("date",
                CalendarDateFormatter.toDateTimeStringISO(pointFeat.getObservationTimeAsCalendarDate()));

        staxWriter.writeCharacters("\n        ");
        staxWriter.writeEmptyElement("location");
        staxWriter.writeAttribute("latitude", Format.dfrac(loc.getLatitude(), 3));
        staxWriter.writeAttribute("longitude", Format.dfrac(loc.getLongitude(), 3));
        if (!Double.isNaN(loc.getAltitude()))
            staxWriter.writeAttribute("altitude", Format.dfrac(loc.getAltitude(), 0));

        StructureData structureData = pointFeat.getDataAll();
        for (VariableSimpleIF wantedVar : wantedVariables) {
            staxWriter.writeCharacters("\n        ");
            staxWriter.writeStartElement("data");
            staxWriter.writeAttribute("name", wantedVar.getShortName());
            if (wantedVar.getUnitsString() != null)
                staxWriter.writeAttribute(CDM.UNITS, wantedVar.getUnitsString());

            Array dataArray = structureData.getArray(wantedVar.getShortName());
            String ss = dataArray.toString();
            Class elemType = dataArray.getElementType();
            if ((elemType == String.class) || (elemType == char.class) || (elemType == StructureData.class))
                ss = ucar.nc2.util.xml.Parse.cleanCharacterData(ss); // make sure no bad chars
            staxWriter.writeCharacters(ss.trim());
            staxWriter.writeEndElement();
        }

        staxWriter.writeCharacters("\n    ");
        staxWriter.writeEndElement();
    }

    @Override
    public void writeFooter() throws XMLStreamException {
        staxWriter.writeCharacters("\n");
        staxWriter.writeEndElement();
        staxWriter.writeCharacters("\n");
        staxWriter.writeEndDocument();

        staxWriter.close();  // This should flush the writer. The underlying output stream remains open.
    }
}
