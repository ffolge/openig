/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openig.audit.decoration;

import static org.forgerock.openig.el.Bindings.bindings;

import java.util.Set;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.ResultHandler;

/**
 * Intercept execution flow and send audit notifications with relevant tags.
 */
@Deprecated
class AuditFilter extends AuditBaseObject implements Filter {
    private final Filter delegate;


    public AuditFilter(final org.forgerock.openig.audit.AuditSystem auditSystem,
                       final org.forgerock.openig.audit.AuditSource source,
                       final Filter delegate,
                       final Set<String> additionalTags) {
        super(source, auditSystem, additionalTags);
        this.delegate = delegate;

    }

    @Override
    public Promise<Response, NeverThrowsException> filter(final Context context,
                                                          final Request request,
                                                          final Handler next) {
        fireAuditEvent(bindings(context, request), requestTags);
        return delegate.filter(context, request, next)
                       .thenOnResult(new ResultHandler<Response>() {
                           @Override
                           public void handleResult(final Response response) {
                               fireAuditEvent(bindings(context, request, response), completedResponseTags);
                           }
                       });
    }
}
