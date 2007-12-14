/**
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */




package ucar.unidata.repository;


import org.w3c.dom.*;


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DefaultOutputHandler extends OutputHandler {


    /** _more_ */
    public static final String OUTPUT_XML = "default.xml";

    /** _more_ */
    public static final String OUTPUT_TIMELINE = "default.timeline";


    /** _more_ */
    public static final String OUTPUT_CSV = "default.csv";

    /** _more_ */
    public static final String OUTPUT_GRAPH = "default.graph";

    /** _more_ */
    public static final String OUTPUT_CLOUD = "default.cloud";



    /**
     * _more_
     *
     *
     *
     * @param args _more_
     * @throws Exception _more_
     */
    public DefaultOutputHandler(Repository repository,Element element) throws Exception {
        super(repository,element);
    }

    public boolean canHandle(Request request)  {
        String output = (String) request.getOutput();
        return output.equals(OUTPUT_HTML) ||
            output.equals(OUTPUT_TIMELINE) ||
            output.equals(OUTPUT_XML) ||
            output.equals(OUTPUT_CSV) ||
            output.equals(OUTPUT_GRAPH) ||
            output.equals(OUTPUT_CLOUD) ||
            output.equals(OUTPUT_ZIP);
    }


    protected List getOutputTypesFor(Request request, String what) throws Exception {
        List list = new ArrayList();
        if(what.equals(WHAT_ENTRIES)) {
            list.add(new TwoFacedObject("Html",OUTPUT_HTML));
            if(repository.isAppletEnabled(request)) {
                list.add(new TwoFacedObject("Timeline",OUTPUT_TIMELINE));
            }
            list.add(new TwoFacedObject("CSV",OUTPUT_CSV));
        }  else  if(what.equals(WHAT_TAG)) {
            list.add(new TwoFacedObject("Tag Html",OUTPUT_HTML));
            list.add(new TwoFacedObject("Tag Cloud",OUTPUT_CLOUD));
            list.add(new TwoFacedObject("Tag XML",OUTPUT_XML));
            list.add(new TwoFacedObject("Tag CSV",OUTPUT_CSV));
        }  else  if(what.equals(WHAT_TYPE)) {
            list.add(new TwoFacedObject("Type Html",OUTPUT_HTML));
            list.add(new TwoFacedObject("Type XML",OUTPUT_XML));
            list.add(new TwoFacedObject("Type CSV",OUTPUT_CSV));
        }  else  if(what.equals(WHAT_GROUP)) {
            list.add(new TwoFacedObject("Group Html",OUTPUT_HTML));
            if(repository.isAppletEnabled(request)) {
                list.add(new TwoFacedObject("Timeline",OUTPUT_TIMELINE));
            }
            list.add(new TwoFacedObject("Group XML",OUTPUT_XML));
            list.add(new TwoFacedObject("Group CSV",OUTPUT_CSV));
        } else {
            list.add(new TwoFacedObject("Html",OUTPUT_HTML));
            list.add(new TwoFacedObject("XML",OUTPUT_XML));
            list.add(new TwoFacedObject("CSV",OUTPUT_CSV));
        }
        return list;
    }

    protected List getOutputTypesForEntries(Request request) throws Exception {
        List list = new ArrayList();
        list.add(new TwoFacedObject("Html",OUTPUT_HTML));
        list.add(new TwoFacedObject("Html with timeline",OUTPUT_TIMELINE));
        list.add(new TwoFacedObject("CSV",OUTPUT_CSV));
        list.add(new TwoFacedObject("Zip File",OUTPUT_ZIP));
        return list;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processShowEntry(Request request, Entry entry) throws Exception {
        TypeHandler typeHandler = repository.getTypeHandler(entry.getType());
        StringBuffer sb = typeHandler.getEntryContent(entry, request);
        return new Result("Entry: " + entry.getName(), sb,
                          getMimeType(request.getOutput()));
    }


    protected Result listGroups(Request request, List<Group> groups) throws Exception {
        StringBuffer sb        = new StringBuffer();
        String       output    = request.getOutput();
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h3>Groups</h3>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER);
            sb.append("\n");
            sb.append(XmlUtil.openTag(TAG_GROUPS));

        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        for (Group group: groups) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>" +getGroupLinks(request, group) +" " +group.getFullName());
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_GROUP,
                                      XmlUtil.attrs(ATTR_NAME,
                                          group.getFullName(), ATTR_ID,
                                          group.getId())));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(group.getFullName(), group.getId()));
                sb.append("\n");
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>\n");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_GROUPS));
        }
        return new Result("", sb, getMimeType(output));
    }



    public void getEntryHtml(StringBuffer sb, List<Entry> entries, Request request, boolean doForm, boolean dfltSelected)  throws Exception {
        if(doForm) {
            sb.append(HtmlUtil.form(repository.href("/getentries"), "getentries"));
            sb.append(HtmlUtil.submit("Get selected","getselected"));
            sb.append(HtmlUtil.submit("Get all","getall"));
            sb.append(" As:");
            List outputList =  repository.getOutputTypesForEntries(request);
            sb.append(HtmlUtil.select(ARG_OUTPUT, outputList));
            sb.append("<p>\n");
            sb.append("<ul>\n");

        }
        for(Entry entry: entries) {
            sb.append(HtmlUtil.checkbox("entry_" + entry.getId(), "true",dfltSelected));
            sb.append(HtmlUtil.hidden("all_" + entry.getId(), "1"));
            sb.append(" ");
            sb.append(entry.getTypeHandler().getEntryLinks(entry,request));
            sb.append(" ");
            sb.append(repository.href(HtmlUtil.url("/showentry", ARG_ID, entry.getId()),
                                      entry.getName()));
            sb.append("<br>\n");
        }
        if(doForm) {
            sb.append("</ul>");
            sb.append("</form>");
        }
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listGroups(Request request) throws Exception {
        TypeHandler typeHandler = repository.getTypeHandler(request);
        Statement    statement =  typeHandler.executeSelect(request,
                                                            SqlUtil.distinct(COL_ENTRIES_GROUP_ID));
        String[]     groups    = SqlUtil.readString(statement, 1);
        StringBuffer sb        = new StringBuffer();
        String       output    = request.getOutput();
        if (output.equals(OUTPUT_HTML)) {
            sb.append("<h3>Groups</h3>");
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_GROUPS));

        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        for (int i = 0; i < groups.length; i++) {
            Group group = repository.findGroup(groups[i]);
            if (group == null) {
                continue;
            }

            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>" +getGroupLinks(request, group) +" " +group.getFullName());
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_GROUP,
                                      XmlUtil.attrs(ATTR_NAME,
                                          group.getFullName(), ATTR_ID,
                                          group.getId())));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(group.getFullName(), group.getId()));
                sb.append("\n");
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>\n");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_GROUPS));
        }

        return new Result("", sb, getMimeType(output));
    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTypes(Request request,List<TypeHandler> typeHandlers) throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = request.getOutput();
        if (output.equals(OUTPUT_HTML)) {
            appendListHeader(request, output, WHAT_TYPE, sb);
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_TYPES));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        for (TypeHandler theTypeHandler : typeHandlers) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li>");
                sb.append(
                    repository.href(HtmlUtil.url(
                        "/searchform", ARG_TYPE,
                        theTypeHandler.getType()), HtmlUtil.img(repository.getUrlBase() + "/Search16.gif", "Search in Group")));
                sb.append(" ");
                sb.append(
                    repository.href(HtmlUtil.url(
                        "/list/home", ARG_TYPE,
                        theTypeHandler.getType()), theTypeHandler.getType()));
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_TYPE,
                                      XmlUtil.attrs(ATTR_TYPE,
                                          theTypeHandler.getType())));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(theTypeHandler.getType(),
                                        theTypeHandler.getDescription()));
                sb.append("\n");
            }

        }
        if (output.equals(OUTPUT_HTML)) {
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_TYPES));
        }
        return new Result("", sb, getMimeType(output));
    }


    protected void appendListHeader(Request request, String output, String what, StringBuffer sb) 
        throws Exception {
        List<TwoFacedObject>  outputTypes = repository.getOutputTypesFor(request, what);
        int cnt = 0;
        sb.append("<b>");
        String initialOutput =  request.getOutput((String)null);
        for(TwoFacedObject tfo: outputTypes) {
            if(cnt++>0) sb.append("&nbsp;|&nbsp;");
            request.put(ARG_OUTPUT, (String)tfo.getId());
            if(tfo.getId().equals(output)) {
                sb.append(HtmlUtil.span(tfo.toString(),""));
            } else {
                sb.append(HtmlUtil.href(request.getType()+"?"+request.getUrlArgs(),tfo.toString()));
            }
        }
        if(initialOutput!=null)   request.put(ARG_OUTPUT,initialOutput);
        sb.append("</b>");

    }


    protected List getEntriesHeader(Request request, String output, String what) 
        throws Exception {
        List<TwoFacedObject>  outputTypes = repository.getOutputTypesFor(request, what);
        int cnt = 0;
        List items = new ArrayList();
        for(TwoFacedObject tfo: outputTypes) {
            request.put(ARG_OUTPUT, (String)tfo.getId());
            if(tfo.getId().equals(output)) {
                items.add(tfo.toString());
            } else {
                items.add(HtmlUtil.href(request.getType()+"?"+request.getUrlArgs(),tfo.toString(), " class=\"subnavlink\" "));
            }
        }
        return items;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTags(Request request,List<Tag> tags) throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = request.getOutput();
        if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_CLOUD)) {
            appendListHeader(request, output, WHAT_TAG, sb);
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_TAGS));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }
        request.remove(ARG_OUTPUT);            
        int              max  = -1;
        int              min  = -1;

        for(Tag tag: tags) {
            if ((max < 0) || (tag.getCount() > max)) {
                max = tag.getCount();
            }
            if ((min < 0) || (tag.getCount() < min)) {
                min = tag.getCount();
            }
        }

        int    diff         = max - min;
        double distribution = diff / 5.0;

        for(Tag tag: tags) {
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li> ");
                sb.append(repository.getTagLinks(request, tag.getName()));
                sb.append(" ");
                sb.append(tag.getName());
                sb.append(" (" + tag.getCount() + ")");

            } else if (output.equals(OUTPUT_CLOUD)) {
                double percent = tag.getCount() / distribution;
                int    bin     = (int) (percent * 5);
                String css     = "font-size:" + (12 + bin * 2);
                sb.append("<span style=\"" + css + "\">");
                String extra = XmlUtil.attrs("alt", "Count:" + tag.getCount(),
                                             "title", "Count:" + tag.getCount());
                sb.append(repository.href(HtmlUtil.url("/graphview", ARG_ID, tag.getName(), ARG_NODETYPE,
                                            TYPE_TAG), tag.getName(), extra));
                sb.append("</span>");
                sb.append(" &nbsp; ");
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_TAG,
                                      XmlUtil.attrs(ATTR_NAME, tag.getName())));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(tag.getName());
                sb.append("\n");
            }
        }

        String pageTitle = "";
        if (output.equals(OUTPUT_HTML)) {
            if(tags.size()==0)
                sb.append("No tags found");
            pageTitle = "Tags";
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            if(tags.size()==0)
                sb.append("No tags found");
            pageTitle = "Tag Cloud";
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_TAGS));
        }
        Result result = new Result(pageTitle, sb, getMimeType(output));
        //        StringBuffer  tsb = new StringBuffer();
        //        appendListHeader(request, output, WHAT_TAG, tsb);
        //        result.putProperty(PROP_NAVSUBLINKS, 
        return result;
    }


    public String getMimeType(String output) {
        if (output.equals(OUTPUT_CSV)) {
            return repository.getMimeTypeFromSuffix(".csv");
        } else if (output.equals(OUTPUT_XML)) {
            return repository.getMimeTypeFromSuffix(".xml");
        } else if (output.equals(OUTPUT_TIMELINE)) {
            return repository.getMimeTypeFromSuffix(".html");
        } else if (output.equals(OUTPUT_GRAPH)) {
            return repository.getMimeTypeFromSuffix(".xml");
        } else if (output.equals(OUTPUT_HTML)) {
            return repository.getMimeTypeFromSuffix(".html");
        } else if (output.equals(OUTPUT_CLOUD)) {
            return repository.getMimeTypeFromSuffix(".html");
        } else if (output.equals(OUTPUT_ZIP)) {
            return repository.getMimeTypeFromSuffix(".zip");
        } else {
            return super.getMimeType(output);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listAssociations(Request request) throws Exception {
        StringBuffer sb     = new StringBuffer();
        String       output = request.getOutput();
        if (output.equals(OUTPUT_HTML)) {
            appendListHeader(request, output, WHAT_ASSOCIATION, sb);
            sb.append("<ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            sb.append("<h3>Association Cloud</h3>");
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.XML_HEADER + "\n");
            sb.append(XmlUtil.openTag(TAG_ASSOCIATIONS));
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }
        TypeHandler typeHandler = repository.getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        if (where.size() > 0) {
            where.add(0, SqlUtil.eq(COL_ASSOCIATIONS_FROM_ENTRY_ID, COL_ENTRIES_ID));            
            where.add(0, SqlUtil.eq(COL_ASSOCIATIONS_TO_ENTRY_ID, COL_ENTRIES_ID));
        }


        String[] associations = SqlUtil.readString(typeHandler.executeSelect(request,
                                                                             SqlUtil.distinct(COL_ASSOCIATIONS_NAME),
                                                                             where),1);

        
        if(associations.length==0) {
            if (output.equals(OUTPUT_HTML) ||
                output.equals(OUTPUT_CLOUD)) {
                sb.append("No associations found");
            }
        }
        List<String>     names  = new ArrayList<String>();
        List<Integer>    counts = new ArrayList<Integer>();
        ResultSet        results;
        int              max  = -1;
        int              min  = -1;
        for(int i=0;i<associations.length;i++) {
            String association   = associations[i];
            Statement stmt2 = typeHandler.executeSelect(request,
                                                        SqlUtil.count("*"),
                                                        Misc.newList(SqlUtil.eq(COL_ASSOCIATIONS_NAME,SqlUtil.quote(association))));

            ResultSet results2 = stmt2.getResultSet();
            if(!results2.next()) continue;
            int    count = results2.getInt(1);
            if ((max < 0) || (count > max)) {
                max = count;
            }
            if ((min < 0) || (count < min)) {
                min = count;
            }
            names.add(association);
            counts.add(new Integer(count));
        }

        int    diff         = max - min;
        double distribution = diff / 5.0;

        for (int i = 0; i < names.size(); i++) {
            String association   = names.get(i);
            int    count = counts.get(i).intValue();
            if (output.equals(OUTPUT_HTML)) {
                sb.append("<li> ");
                sb.append(repository.getAssociationLinks(request, association));
                sb.append(" ");
                sb.append(association);
                sb.append(" (" + count + ")");

            } else if (output.equals(OUTPUT_CLOUD)) {
                double percent = count / distribution;
                int    bin     = (int) (percent * 5);
                String css     = "font-size:" + (12 + bin * 2);
                sb.append("<span style=\"" + css + "\">");
                String extra = XmlUtil.attrs("alt", "Count:" + count,
                                             "title", "Count:" + count);
                sb.append(repository.href(HtmlUtil.url("/graphview", ARG_ID, association, ARG_NODETYPE,
                                            TYPE_ASSOCIATION), association, extra));
                sb.append("</span>");
                sb.append(" &nbsp; ");
            } else if (output.equals(OUTPUT_XML)) {
                sb.append(XmlUtil.tag(TAG_ASSOCIATION,
                                      XmlUtil.attrs(ATTR_NAME, association)));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(association);
                sb.append("\n");
            }
        }

        String pageTitle = "";
        if (output.equals(OUTPUT_HTML)) {
            pageTitle = "Associations";
            sb.append("</ul>");
        } else if (output.equals(OUTPUT_CLOUD)) {
            pageTitle = "Association Cloud";
        } else if (output.equals(OUTPUT_XML)) {
            sb.append(XmlUtil.closeTag(TAG_ASSOCIATIONS));
        }
        return new Result(pageTitle, sb, getMimeType(output));
    }





    public Result processShowGroup(Request request,Group group, List<Group> subGroups, List<Entry>entries) throws Exception {
        String       output = request.getOutput();
        if(output.equals(OUTPUT_XML) || output.equals(OUTPUT_CSV)) return listGroups(request, subGroups);
        boolean showApplet = repository.isAppletEnabled(request);
        String  title = group.getFullName();
        StringBuffer sb     = new StringBuffer();
        if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)) {
            if(output.equals(OUTPUT_HTML)) {
                showApplet = false;
            } else {
                //                output = OUTPUT_TIMELINE;
            }
            //xxxxx
            //            appendListHeader(request, output, WHAT_GROUP, sb);
            sb.append("<p>\n");
            String[] crumbs = getBreadCrumbs(request, group,false);
            title = crumbs[0];
            sb.append(HtmlUtil.bold("Group: ")+ crumbs[1]);
            List<Metadata> metadataList = repository.getMetadata(group);
            if(metadataList.size()>0) {
                sb.append("<p>");
                sb.append(HtmlUtil.bold("Metadata:"));
                sb.append("<ul>");
                for(Metadata metadata:  metadataList) {
                    sb.append("<li>");
                    sb.append(metadata.getName());
                    sb.append("<br>");
                    sb.append(metadata.getContent());
                }
            }


            //            sb.append("<hr>");
            sb.append("<p>");
            if (subGroups.size() > 0) {
                sb.append(HtmlUtil.bold("Sub groups:"));
                sb.append("<ul>");
                for (Group subGroup : subGroups) {
                    sb.append(getGroupLinks(request, subGroup) + " " 
                              + repository.href(HtmlUtil
                                     .url("/showgroup", ARG_GROUP,
                                          subGroup.getFullName(),ARG_OUTPUT,output), subGroup
                                     .getName()) + "</a>");

                    sb.append("\n<br>\n");
                }
                sb.append("</ul>");
            }
            if(entries.size()>0) {
                sb.append("\n");
                sb.append(HtmlUtil.bold("Entries:"));
                if (entries.size() > 0 && showApplet){
                    sb.append(getTimelineApplet(request, entries));
                }
                sb.append("<br>");
                getEntryHtml(sb,entries,request,true,false);
            }
        }

        Result result = new Result(title, sb, getMimeType(output));
        result.putProperty(PROP_NAVSUBLINKS,  getEntriesHeader(request,  output,WHAT_GROUP)); 
        return result;
    }




    public Result processShowGroups(Request request,List<Group> groups) throws Exception {
        String       output = request.getOutput();
        if(output.equals(OUTPUT_XML) || output.equals(OUTPUT_CSV)) return listGroups(request, groups);
        StringBuffer sb     = new StringBuffer();
        String title = "Groups";
        
        if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)) {
            appendListHeader(request, output, WHAT_GROUP, sb);
            sb.append("<ul>");
        }

        for (Group group : groups) {
            if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)) {
                sb.append(
                          getGroupLinks(request, group) + " " );
                sb.append(repository.href(HtmlUtil.url(
                                                       "/showgroup", ARG_OUTPUT, output,ARG_GROUP,
                                                       group.getFullName()), group.getFullName()) + "</a> "
                          );
                sb.append("\n<br>\n");
            }
        }
        Result result = new Result(title, sb, getMimeType(output));
        result.putProperty(PROP_NAVSUBLINKS,  getEntriesHeader(request,  output,WHAT_GROUP)); 
        return result;
    }




    protected String getTimelineApplet(Request request, List<Entry> entries) throws Exception {
        timelineAppletTemplate = IOUtil.readContents(repository.getProperty(PROP_HTML_TIMELINEAPPLET), getClass());
        List         times   = new ArrayList();
        List         labels  = new ArrayList();
        List         ids     = new ArrayList();
        for (Entry entry : entries) {
            times.add(SqlUtil.format(new Date(entry.getStartDate())));
            labels.add(entry.getName());
            ids.add(entry.getId());
        }
        String tmp = StringUtil.replace(timelineAppletTemplate,
                                        "%times%", StringUtil.join(",", times));
        tmp = StringUtil.replace(tmp, "%labels%",
                                 StringUtil.join(",", labels));
        tmp = StringUtil.replace(tmp, "%ids%",
                                 StringUtil.join(",", ids));
        tmp = StringUtil.replace(tmp, "%loadurl%",
                                 repository.href(HtmlUtil.url("/getentries",ARG_IDS,"%ids%", ARG_OUTPUT,OUTPUT_HTML)));
        return tmp;

    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntries(Request request, List<Entry> entries) throws Exception {



        StringBuffer sb      = new StringBuffer();
        String       output  = request.getOutput();
        boolean showApplet = repository.isAppletEnabled(request);
        if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)) {
            //appendEntriesHeader(request,  output,  sb) ;
            sb.append("<p>\n");
            if (entries.size() == 0) {
                sb.append("<b>Nothing Found</b><p>");
            }
            sb.append("<table>");

            if (output.equals(OUTPUT_TIMELINE)) {
            } else {
                showApplet = false;
            }
        } else if (output.equals(OUTPUT_ZIP)) {
            return toZip(request, entries);
        } else if (output.equals(OUTPUT_CSV)) {}
        else {
            throw new IllegalArgumentException("Unknown output type:"
                    + output);
        }

        StringBufferCollection sbc = new StringBufferCollection();
        for (Entry entry : entries) {
            StringBuffer ssb = sbc.getBuffer(entry.getTypeHandler().getDescription());
            if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)) {
                String links = HtmlUtil.checkbox("entry_" + entry.getId(),
                                                 "true") + " " + entry.getTypeHandler().getEntryLinks(entry,request);

                ssb.append(HtmlUtil.hidden("all_" + entry.getId(), "1"));
                ssb.append(HtmlUtil
                    .row(links + " "
                         + repository.href(HtmlUtil
                             .url("/showentry", ARG_ID, entry.getId()), entry
                             .getName()), "" + new Date(entry
                             .getStartDate())));
            } else if (output.equals(OUTPUT_CSV)) {
                sb.append(SqlUtil.comma(entry.getId(), entry.getFile()));
            }
        }


        if (output.equals(OUTPUT_HTML)  || output.equals(OUTPUT_TIMELINE)) {
            if (entries.size() > 0 && showApplet){
                sb.append(getTimelineApplet(request, entries));
            }
            sb.append(HtmlUtil.form(repository.href("/getentries"), "name=\"getentries\" method=\"post\""));
            if (entries.size() > 0) {
                sb.append(HtmlUtil.submit("Get selected", "getselected"));
                sb.append(HtmlUtil.submit("Get all", "getall"));
                sb.append(" As: ");
                List outputList =  repository.getOutputTypesForEntries(request);
                sb.append(HtmlUtil.select(ARG_OUTPUT, outputList));
            }
            sb.append("<br>");
        }
        for (int i = 0; i < sbc.getKeys().size(); i++) {
            String       type = (String) sbc.getKeys().get(i);
            StringBuffer ssb  = sbc.getBuffer(type);
            if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)) {
                sb.append(HtmlUtil.row(HtmlUtil.bold("Type:" + type)));
                sb.append(ssb);
            }
        }

        if (output.equals(OUTPUT_HTML) || output.equals(OUTPUT_TIMELINE)) {
            sb.append("</form>");
            sb.append("</table>");

        }
        Result result = new Result("Query Results", sb,
                                   getMimeType(output));
        result.putProperty(PROP_NAVSUBLINKS, getEntriesHeader(request,  output,WHAT_ENTRIES)); 

        //        result.putProperty(PROP_NAVSUBLINKS, repository.getSearchFormLinks(request));
        return result;

    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result toZip(Request request, List<Entry> entries)
            throws Exception {
        ByteArrayOutputStream bos  = new ByteArrayOutputStream();
        ZipOutputStream       zos  = new ZipOutputStream(bos);
        Hashtable             seen = new Hashtable();
        for (Entry entry : entries) {
            String path = entry.getFile();
            String name = IOUtil.getFileTail(path);
            int    cnt  = 1;
            while (seen.get(name) != null) {
                name = (cnt++) + "_" + name;
            }
            seen.put(name, name);
            zos.putNextEntry(new ZipEntry(name));
            byte[] bytes = IOUtil.readBytes(IOUtil.getInputStream(path,
                               getClass()));
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }
        zos.close();
        bos.close();
        return new Result("", bos.toByteArray(),
                          getMimeType(OUTPUT_ZIP));
    }


}

