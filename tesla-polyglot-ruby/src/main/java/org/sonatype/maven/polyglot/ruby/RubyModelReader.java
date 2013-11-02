/**
 * Copyright (c) 2012 to original author or authors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.sonatype.maven.polyglot.ruby;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.maven.polyglot.Constants;
import org.sonatype.maven.polyglot.PolyglotModelUtil;
import org.sonatype.maven.polyglot.execute.ExecuteManager;
import org.sonatype.maven.polyglot.io.ModelReaderSupport;

/**
 * Reads a <tt>pom.rb</tt> and transforms into a Maven {@link Model}.
 *
 * @author m.kristian
 */
@Component(role = ModelReader.class, hint="ruby")
public class RubyModelReader extends ModelReaderSupport {

    @Requirement
    ExecuteManager executeManager;
    
    @Requirement( hint = "ruby" )
    SetupClassRealm setupManager;
    
    public Model read( final Reader input, final Map<String, ?> options )
            throws IOException {
        assert input != null;

        // use the classloader which loaded that class here
        // for testing that classloader does not need to be a ClassRealm, i.e. the test setup needs 
        // to take care that all classes are in place
        if ( getClass().getClassLoader() instanceof ClassRealm ) {
            // TODO get that version out of here !!!
            setupManager.setupArtifact( Constants.getGAV( "ruby" ),
                                        (ClassRealm) getClass().getClassLoader() );
        }
        
        ClassLoader old = null;
        try {
            // make sure jruby will find the right classloader
            old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );

            // read the stream from our pom.rb into a String
            StringWriter ruby = new StringWriter();
            IOUtil.copy( input, ruby );
        
            // parse the String and create a POM model
            return new RubyParser( executeManager ).parse( ruby.toString(),
                                                           PolyglotModelUtil.getLocationFile( options ),
                                                           options );
        }
        finally {
            Thread.currentThread().setContextClassLoader( old );
        }
    }
}
