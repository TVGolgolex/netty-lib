package de.mariokurz.nettylib.network.protocol;

/*
 * MIT License
 *
 * Copyright (c) 2024 22:14 Mario Pascal K.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.google.gson.JsonObject;
import de.golgolex.quala.json.JsonUtils;
import de.golgolex.quala.json.document.JsonDocument;
import de.mariokurz.nettylib.NettyLib;
import de.mariokurz.nettylib.network.protocol.routing.RoutingPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.logging.Level;

@Getter
@AllArgsConstructor
public class JsonPacket extends RoutingPacket implements Serializable {

    private final String json;

    public JsonPacket(JsonDocument jsonDocument) {
        this.json = JsonUtils.toJson(jsonDocument.jsonObject());
    }

    public JsonDocument jsonDocument() {
        if (this.json == null) {
            NettyLib.log(Level.SEVERE, this.getClass(), "No Json string provided");
            return new JsonDocument();
        }
        return new JsonDocument(JsonUtils.fromJson(this.json, JsonObject.class));
    }
}
