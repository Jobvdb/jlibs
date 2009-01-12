/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.xml.sax.sniff;

import jlibs.xml.sax.SAXDebugHandler;
import jlibs.xml.sax.SAXUtil;
import jlibs.xml.sax.sniff.model.Node;
import jlibs.xml.sax.sniff.model.Root;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * @author Santhosh Kumar T
 */
public class Sniffer extends DefaultHandler{
    static boolean debug = XMLDog.debug;
    private Root root;

    public Sniffer(Root root){
        this.root = root;
        elementStack = new ElementStack(root);
        if(debug)
            root.print();
    }

    private StringContent contents = new StringContent();
    private PositionStack positionStack = new PositionStack();
    private ElementStack elementStack;
    private XPathResults results;

    /*-------------------------------------------------[ Events ]---------------------------------------------------*/
    
    @Override
    public void startDocument() throws SAXException{
        if(debug)
            System.out.println("-----------------------------------------------------------------");

        contexts.clear();
        newContexts.clear();
        
        contents.reset();
        positionStack.reset();
        elementStack.reset();
        
        contexts.add(new Context(root));

        println("Contexts", contexts);
        if(debug)
            System.out.println("-----------------------------------------------------------------");
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException{
        if(debug)
            System.out.println();
        
        int pos = positionStack.push(uri, localName);
        elementStack.push(uri, localName, pos);

        for(Context context: contexts){
            context.matchText(contents);
            context.startElement(uri, localName, pos);
        }
        contents.reset();
        updateContexts();

        // match attributes
        for(Context newContext: contexts)
            newContext.matchAttributes(attrs);

        if(debug)
            System.out.println("-----------------------------------------------------------------");
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException{
        contents.write(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException{
        if(debug)
            System.out.println();
        
        positionStack.pop();
        elementStack.pop();
        
        for(Context context: contexts){
            context.matchText(contents);
            Sniffer.Context newContext = context.endElement();
            if(!newContexts.contains(newContext))
                newContexts.add(newContext);
        }
        contents.reset();
        updateContexts();

        if(debug)
            System.out.println("-----------------------------------------------------------------");
    }

    /*-------------------------------------------------[ Contexts ]---------------------------------------------------*/
    
    private List<Context> contexts = new ArrayList<Context>();
    private List<Context> newContexts = new ArrayList<Context>();

    private void println(String message, List<Context> contexts){
        println(message, contexts, 0);
    }

    private void println(String message, List<Context> contexts, int from){
        if(debug){
            System.out.println(message+" ->");
            if(contexts.size()==from)
                return;
            Iterator iter = contexts.listIterator(from);
            while(iter.hasNext())
                System.out.println("     "+iter.next());
            System.out.println();
        }
    }
    
    private void updateContexts(){
        List<Context> temp = contexts;
        contexts = newContexts;
        newContexts = temp;
        newContexts.clear();
        println("newContext", contexts);
    }

    static int maxInstCount;
    static int instCount;
    class Context{
        Context parent;
        Node node;
        int depth;
//        int parentDepths[];

        private Context(){
            instCount++;
            maxInstCount = Math.max(maxInstCount, instCount);
        }
        
        Context(Root root){
            this();
//            parentDepths = new int[0];
            node = root;
        }

        public Context childContext(Node child){
            Context context = new Context();
            context.node = child;
            context.parent = this;

//            context.parentDepths = new int[parentDepths.length+1];
//            System.arraycopy(parentDepths, 0, context.parentDepths, 0, parentDepths.length);
//            context.parentDepths[context.parentDepths.length-1] = depth;
            
            return context;
        }

        public Context parentContext(){
            instCount--;
            return parent;

//            Context parent = new Context();
//            parent.node = node.parent;
//            parent.depth = parentDepths[parentDepths.length-1];
//
//            parent.parentDepths = new int[parentDepths.length-1];
//            System.arraycopy(parentDepths, 0, parent.parentDepths, 0, parent.parentDepths.length);
//            return parent;
        }

        public void matchText(StringContent contents){
            if(!contents.isEmpty() && depth<=0){
                for(Node child: node.children){
                    if(child.matchesText(contents))
                        results.hit(child, contents);
                    checkConstraints(child, contents);
                }
            }
        }

        private void checkConstraints(Node child, String uri, String name, int pos, List<Context> newContexts){
            for(Node constraint: child.constraints){
                if(constraint.matchesElement(uri, name, pos)){
                    results.hit(constraint, elementStack);
                    newContexts.add(childContext(constraint));
                    checkConstraints(constraint, uri, name, pos, newContexts);
                }
            }
        }
        
        private void checkConstraints(Node child, StringContent contents){
            for(Node constraint: child.constraints){
                if(constraint.matchesText(contents)){
                    results.hit(constraint, contents);
                    checkConstraints(constraint, contents);
                }
            }
        }

        public List<Context> startElement(String uri, String name, int pos){
            String message = toString();

            int contextsSize = newContexts.size();

            if(depth>0){
                depth++;
                newContexts.add(this);
            }else{
                for(Node child: node.children){
                    if(child.matchesElement(uri, name, pos)){
                        results.hit(child, elementStack);
                        newContexts.add(childContext(child));
                        checkConstraints(child, uri, name, pos, newContexts);
                    }
                }
                if(node.consumable()){
                    depth--;
                    newContexts.add(this);
                    checkConstraints(node, uri, name, pos, newContexts);
                }else{
                    if(newContexts.size()==0){
                        depth++;
                        newContexts.add(this);
                    }
                }
            }

            println(message, newContexts, contextsSize);
            return newContexts;
        }

        public void matchAttributes(Attributes attrs){
            if(depth<=0){
                for(int i=0; i<attrs.getLength(); i++){
                    String uri = attrs.getURI(i);
                    String name = attrs.getLocalName(i);
                    String value = attrs.getValue(i);

                    for(Node child: node.children){
                        if(child.matchesAttribute(uri, name, value)){
                            results.hit(child, value);
                            for(Node constraint: child.constraints){
                                if(constraint.matchesAttribute(uri, name, value))
                                    results.hit(constraint, value);
                            }
                        }
                    }
                }
            }
        }

        public Context endElement(){
            if(depth==0)
                return parentContext();
            else{
                if(depth>0)
                    depth--;
                else
                    depth++;
                
                return this;
            }
        }

        @Override
        public int hashCode(){
            return depth + node.hashCode();
        }

        @Override
        public boolean equals(Object obj){
            if(obj instanceof Context){
                Context that = (Context)obj;
                return this.depth==that.depth && this.node==that.node;
            }else
                return false;
        }

        @Override
        public String toString(){
            return "["+depth+"] "+node;
        }
    }

    /*-------------------------------------------------[ Sniffing ]---------------------------------------------------*/

    public XPathResults sniff(InputSource source, int minHits) throws ParserConfigurationException, SAXException, IOException{
        try{
            results = new XPathResults(minHits);
            DefaultHandler handler = this;
            if(debug)
                handler = new SAXDebugHandler(handler);
            SAXUtil.newSAXParser(true, false).parse(source, handler);
        }catch(RuntimeException ex){
            if(ex!=XPathResults.STOP_PARSING)
                throw ex;
            if(debug)
                System.out.println("COMPLETE DOCUMENT IS NOT PARSED !!!");
        }
        if(debug){
            System.out.println("max contexts: "+maxInstCount);
        }
        return results;
    }
}