// $Id:
// FORESTER -- software libraries and applications
// for evolutionary biology research and applications.
//
// Copyright (C) 2008-2009 Christian M. Zmasek
// Copyright (C) 2008-2009 Burnham Institute for Medical Research
// Copyright (C) 2000-2001 Washington University School of Medicine
// and Howard Hughes Medical Institute
// All rights reserved
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
//
// Contact: phylosoft @ gmail . com
// WWW: www.phylosoft.org/forester

package org.forester.application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.forester.datastructures.IntMatrix;
import org.forester.io.parsers.phyloxml.PhyloXmlParser;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.factories.ParserBasedPhylogenyFactory;
import org.forester.phylogeny.factories.PhylogenyFactory;
import org.forester.sdi.RIO;
import org.forester.util.CommandLineArguments;
import org.forester.util.EasyWriter;
import org.forester.util.ForesterUtil;

public class rio {

    final static private String PRG_NAME              = "rio";
    final static private String PRG_VERSION           = "3.00 beta 1";
    final static private String PRG_DATE              = "2012.11.27";
    final static private String E_MAIL                = "czmasek@burnham.org";
    final static private String WWW                   = "www.phylosoft.org/forester/";
    final static private String HELP_OPTION_1         = "help";
    final static private String HELP_OPTION_2         = "h";
    final static private String QUERY_OPTION          = "q";
    final static private String SORT_OPTION           = "s";
    final static private String OUTPUT_ULTRA_P_OPTION = "u";
    final static private String CUTOFF_ULTRA_P_OPTION = "cu";
    final static private String CUTOFF_ORTHO_OPTION   = "co";
    final static private String TABLE_OUTPUT_OPTION   = "t";

    public static void main( final String[] args ) {
        ForesterUtil.printProgramInformation( PRG_NAME,
                                              "resampled inference of orthologs",
                                              PRG_VERSION,
                                              PRG_DATE,
                                              E_MAIL,
                                              WWW,
                                              ForesterUtil.getForesterLibraryInformation() );
        CommandLineArguments cla = null;
        try {
            cla = new CommandLineArguments( args );
        }
        catch ( final Exception e ) {
            ForesterUtil.fatalError( PRG_NAME, e.getMessage() );
        }
        if ( cla.isOptionSet( HELP_OPTION_1 ) || cla.isOptionSet( HELP_OPTION_2 ) || ( args.length == 0 ) ) {
            printHelp();
        }
        if ( ( args.length < 2 ) || ( args.length > 10 ) ) {
            System.out.println();
            System.out.println( "[" + PRG_NAME + "] incorrect number of arguments" );
            System.out.println();
            printHelp();
        }
        final List<String> allowed_options = new ArrayList<String>();
        allowed_options.add( QUERY_OPTION );
        allowed_options.add( SORT_OPTION );
        allowed_options.add( CUTOFF_ULTRA_P_OPTION );
        allowed_options.add( CUTOFF_ORTHO_OPTION );
        allowed_options.add( TABLE_OUTPUT_OPTION );
        allowed_options.add( OUTPUT_ULTRA_P_OPTION );
        final String dissallowed_options = cla.validateAllowedOptionsAsString( allowed_options );
        if ( dissallowed_options.length() > 0 ) {
            ForesterUtil.fatalError( PRG_NAME, "unknown option(s): " + dissallowed_options );
        }
        final File gene_trees_file = cla.getFile( 0 );
        final File species_tree_file = cla.getFile( 1 );
        File outfile = null;
        if ( cla.getNumberOfNames() > 2 ) {
            outfile = cla.getFile( 2 );
        }
        ForesterUtil.fatalErrorIfFileNotReadable( PRG_NAME, gene_trees_file );
        ForesterUtil.fatalErrorIfFileNotReadable( PRG_NAME, species_tree_file );
        if ( outfile.exists() ) {
            ForesterUtil.fatalError( PRG_NAME, "[" + outfile + "] already exists" );
        }
        String query = null;
        if ( cla.isOptionSet( QUERY_OPTION ) ) {
            query = cla.getOptionValue( QUERY_OPTION );
        }
        File table_outfile = null;
        if ( cla.isOptionSet( TABLE_OUTPUT_OPTION ) ) {
            table_outfile = new File( cla.getOptionValue( TABLE_OUTPUT_OPTION ) );
            if ( table_outfile.exists() ) {
                ForesterUtil.fatalError( PRG_NAME, "[" + table_outfile + "] already exists" );
            }
        }
        boolean output_ultraparalogs = false;
        if ( cla.isOptionSet( OUTPUT_ULTRA_P_OPTION ) ) {
            output_ultraparalogs = true;
        }
        double cutoff_for_orthologs = 50;
        double cutoff_for_ultra_paralogs = 50;
        int sort = 2;
        try {
            if ( cla.isOptionSet( CUTOFF_ORTHO_OPTION ) ) {
                cutoff_for_orthologs = cla.getOptionValueAsDouble( CUTOFF_ORTHO_OPTION );
            }
            if ( cla.isOptionSet( CUTOFF_ULTRA_P_OPTION ) ) {
                cutoff_for_ultra_paralogs = cla.getOptionValueAsDouble( CUTOFF_ULTRA_P_OPTION );
                if ( !output_ultraparalogs ) {
                    printHelp();
                }
            }
            if ( cla.isOptionSet( SORT_OPTION ) ) {
                sort = cla.getOptionValueAsInt( SORT_OPTION );
            }
        }
        catch ( final Exception e ) {
            ForesterUtil.fatalError( PRG_NAME, "error in command line: " + e.getLocalizedMessage() );
        }
        if ( ( cutoff_for_orthologs < 0 ) || ( cutoff_for_ultra_paralogs < 0 ) || ( sort < 0 ) || ( sort > 2 ) ) {
            printHelp();
        }
        if ( ( ( query == null ) && ( outfile != null ) ) || ( ( query != null ) && ( outfile == null ) ) ) {
            printHelp();
        }
        if ( output_ultraparalogs && ( outfile == null ) ) {
            printHelp();
        }
        long time = 0;
        System.out.println( "Gene trees                : " + gene_trees_file );
        System.out.println( "Species tree              : " + species_tree_file );
        if ( query != null ) {
            System.out.println( "Query                     : " + query );
            System.out.println( "Outfile                   : " + outfile );
            System.out.println( "Sort                      : " + sort );
            System.out.println( "Cutoff for  orthologs     : " + cutoff_for_orthologs );
            if ( output_ultraparalogs ) {
                System.out.println( "Cutoff for ultra paralogs : " + cutoff_for_ultra_paralogs );
            }
        }
        if ( table_outfile != null ) {
            System.out.println( "Table output              : " + table_outfile );
        }
        System.out.println();
        time = System.currentTimeMillis();
        Phylogeny species_tree = null;
        try {
            final PhylogenyFactory factory = ParserBasedPhylogenyFactory.getInstance();
            species_tree = factory.create( species_tree_file, new PhyloXmlParser() )[ 0 ];
        }
        catch ( final Exception e ) {
            e.printStackTrace();
            System.exit( -1 );
        }
        if ( !species_tree.isRooted() ) {
            ForesterUtil.printErrorMessage( PRG_NAME, "species tree is not rooted" );
            System.exit( -1 );
        }
        if ( !species_tree.isCompletelyBinary() ) {
            ForesterUtil.printErrorMessage( PRG_NAME, "species tree is not completely binary" );
            System.exit( -1 );
        }
        try {
            RIO rio;
            if ( ForesterUtil.isEmpty( query ) ) {
                rio = new RIO( gene_trees_file, species_tree );
            }
            else {
                rio = new RIO( gene_trees_file, species_tree, query );
            }
            if ( outfile != null ) {
                final StringBuilder output = new StringBuilder();
                output.append( rio.inferredOrthologsToString( query, sort, cutoff_for_orthologs ) );
                if ( output_ultraparalogs ) {
                    output.append( "\n\nUltra paralogs:\n" );
                    output.append( rio.inferredUltraParalogsToString( query, cutoff_for_ultra_paralogs ) );
                }
                output.append( "\n\nSort priority: " + RIO.getOrder( sort ) );
                output.append( "\nExt nodes    : " + rio.getExtNodesOfAnalyzedGeneTrees() );
                output.append( "\nSamples      : " + rio.getNumberOfSamples() + "\n" );
                final PrintWriter out = new PrintWriter( new FileWriter( outfile ), true );
                out.println( output );
                out.close();
            }
            if ( table_outfile != null ) {
                tableOutput( table_outfile, rio );
            }
        }
        catch ( final Exception e ) {
            ForesterUtil.printErrorMessage( PRG_NAME, e.getLocalizedMessage() );
            e.printStackTrace();
            System.exit( -1 );
        }
        if ( outfile != null ) {
            ForesterUtil.programMessage( PRG_NAME, "wrote results to \"" + outfile + "\"" );
        }
        time = System.currentTimeMillis() - time;
        ForesterUtil.programMessage( PRG_NAME, "time: " + time + "ms" );
        ForesterUtil.programMessage( PRG_NAME, "OK" );
        System.exit( 0 );
    }

    private static void tableOutput( final File table_outfile, final RIO rio ) throws IOException {
        final IntMatrix m = RIO.calculateOrthologTable( rio.getAnalyzedGeneTrees() );
        writeTable( table_outfile, rio, m );
    }

    private static void writeTable( final File table_outfile, final RIO rio, final IntMatrix m ) throws IOException {
        final EasyWriter w = ForesterUtil.createEasyWriter( table_outfile );
        final java.text.DecimalFormat df = new java.text.DecimalFormat( "0.###" );
        df.setDecimalSeparatorAlwaysShown( false );
        w.print( "\t" );
        for( int i = 0; i < m.size(); ++i ) {
            w.print( "\t" );
            w.print( m.getLabel( i ) );
        }
        w.println();
        for( int x = 0; x < m.size(); ++x ) {
            w.print( m.getLabel( x ) );
            w.print( "\t" );
            for( int y = 0; y < m.size(); ++y ) {
                w.print( "\t" );
                if ( x == y ) {
                    if ( m.get( x, y ) != rio.getNumberOfSamples() ) {
                        ForesterUtil.unexpectedFatalError( PRG_NAME, "diagonal value is off" );
                    }
                    w.print( "-" );
                }
                else {
                    w.print( df.format( ( ( double ) m.get( x, y ) ) / rio.getNumberOfSamples() ) );
                }
            }
            w.println();
        }
        w.close();
        ForesterUtil.programMessage( PRG_NAME, "wrote table to \"" + table_outfile + "\"" );
    }

    private final static void printHelp() {
        System.out.println( "usage:" );
        System.out.println();
        System.out.println( PRG_NAME + " [options] <gene trees file> <species tree file> [outfile]" );
        System.out.println();
        System.out.println( " options:" );
        System.out.println();
        System.out.println( " -" + CUTOFF_ORTHO_OPTION + " : cutoff for ortholog output (default: 50)" );
        System.out.println( " -" + TABLE_OUTPUT_OPTION + "  : file-name for output table" );
        System.out.println( " -" + QUERY_OPTION + "  : name for query (sequence/node)" );
        System.out.println( " -" + SORT_OPTION + "  : sort (default: 2)" );
        System.out.println( " -" + OUTPUT_ULTRA_P_OPTION
                + "  : to output ultra-paralogs (species specific expansions/paralogs)" );
        System.out.println( " -" + CUTOFF_ULTRA_P_OPTION + " : cutoff for ultra-paralog output (default: 50)" );
        System.out.println();
        System.out.println( " sort:" );
        System.out.println( RIO.getOrderHelp().toString() );
        System.out.println();
        System.out
                .println( " example: \"rio gene_trees.nh species.xml outfile -q=D_HUMAN -t=outtable -u -cu=60 -co=60\"" );
        System.out.println();
        System.exit( -1 );
    }
}
