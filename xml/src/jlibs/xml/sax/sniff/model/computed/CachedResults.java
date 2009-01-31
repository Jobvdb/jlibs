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

package jlibs.xml.sax.sniff.model.computed;

import jlibs.xml.sax.sniff.model.ResultType;
import jlibs.xml.sax.sniff.model.Results;

/**
 * @author Santhosh Kumar T
 */
public class CachedResults extends Results{
    @Override
    public void addResult(int docOrder, String result){
        super.addResult(docOrder, result);

        if(debug)
            System.out.format("CacheHit %2d: %s ---> %s %n", results.size(), this, result);
    }

    public boolean prepareResult(){
        return false;
    }

    public boolean asBoolean(ResultType resultType){
        return resultType.asBoolean(results);
    }

    public String asString(ResultType resultType){
        return resultType.asString(results);
    }

    public double asNumber(ResultType resultType){
        return resultType.asNumber(results);
    }
}