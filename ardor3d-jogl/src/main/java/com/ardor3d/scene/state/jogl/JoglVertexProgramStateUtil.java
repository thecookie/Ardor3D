/**
 * Copyright (c) 2008-2009 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.VertexProgramState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.VertexProgramStateRecord;
import com.ardor3d.util.geom.BufferUtils;

public abstract class JoglVertexProgramStateUtil {
    private static final Logger logger = Logger.getLogger(JoglVertexProgramStateUtil.class.getName());

    /**
     * Queries OpenGL for errors in the vertex program. Errors are logged as SEVERE, noting both the line number and
     * message.
     */
    private static void checkProgramError() {
        final GL gl = GLU.getCurrentGL();

        if (gl.glGetError() == GL.GL_INVALID_OPERATION) {
            // retrieve the error position
            final IntBuffer errorloc = BufferUtils.createIntBuffer(16);
            gl.glGetIntegerv(GL.GL_PROGRAM_ERROR_POSITION_ARB, errorloc); // TODO Check for integer

            logger.severe("Error " + gl.glGetString(GL.GL_PROGRAM_ERROR_STRING_ARB) + " in vertex program on line "
                    + errorloc.get(0));
        }
    }

    private static int create(final ByteBuffer program) {
        final GL gl = GLU.getCurrentGL();

        final IntBuffer buf = BufferUtils.createIntBuffer(1);

        gl.glGenProgramsARB(buf.limit(), buf);
        gl.glBindProgramARB(GL.GL_VERTEX_PROGRAM_ARB, buf.get(0));

        final byte array[] = new byte[program.limit()];
        program.rewind();
        program.get(array);
        gl
                .glProgramStringARB(GL.GL_VERTEX_PROGRAM_ARB, GL.GL_PROGRAM_FORMAT_ASCII_ARB, array.length, new String(
                        array));

        checkProgramError();

        return buf.get(0);
    }

    public static void apply(final JoglRenderer renderer, final VertexProgramState state) {
        final GL gl = GLU.getCurrentGL();
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();

        if (caps.isVertexProgramSupported()) {
            // ask for the current state record
            final VertexProgramStateRecord record = (VertexProgramStateRecord) context
                    .getStateRecord(StateType.VertexProgram);
            context.setCurrentState(StateType.VertexProgram, state);

            if (!record.isValid() || record.getReference() != state) {
                record.setReference(state);
                if (state.isEnabled()) {
                    // Vertex program not yet loaded
                    if (state._getProgramID() == -1) {
                        if (state.getProgramAsBuffer() != null) {
                            final int id = create(state.getProgramAsBuffer());
                            state._setProgramID(id);
                        } else {
                            return;
                        }
                    }

                    gl.glEnable(GL.GL_VERTEX_PROGRAM_ARB);
                    gl.glBindProgramARB(GL.GL_VERTEX_PROGRAM_ARB, state._getProgramID());

                    // load environmental parameters...
                    for (int i = 0; i < VertexProgramState._getEnvParameters().length; i++) {
                        if (VertexProgramState._getEnvParameters()[i] != null) {
                            gl.glProgramEnvParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, i, VertexProgramState
                                    ._getEnvParameters()[i][0], VertexProgramState._getEnvParameters()[i][1],
                                    VertexProgramState._getEnvParameters()[i][2], VertexProgramState
                                            ._getEnvParameters()[i][3]);
                        }
                    }

                    // load local parameters...
                    if (state.isUsingParameters()) {
                        // no parameters are used
                        for (int i = 0; i < state._getParameters().length; i++) {
                            if (state._getParameters()[i] != null) {
                                gl.glProgramLocalParameter4fARB(GL.GL_VERTEX_PROGRAM_ARB, i,
                                        state._getParameters()[i][0], state._getParameters()[i][1], state
                                                ._getParameters()[i][2], state._getParameters()[i][3]);
                            }
                        }
                    }

                } else {
                    gl.glDisable(GL.GL_VERTEX_PROGRAM_ARB);
                }
            }

            if (!record.isValid()) {
                record.validate();
            }
        }
    }
}
