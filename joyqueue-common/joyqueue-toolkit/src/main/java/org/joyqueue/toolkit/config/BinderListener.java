/**
 * Copyright 2019 The JoyQueue Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.joyqueue.toolkit.config;

/**
 * 绑定监听器
 * Created by hexiaofeng on 16-8-29.
 */
public class BinderListener implements Postman.GroupListener {

    Object target;

    public BinderListener(Object target) {
        this.target = target;
    }

    @Override
    public void onUpdate(final String group, final Context context) {
        if (context == null || target == null) {
            return;
        }
        Binders.bind(context, target);
    }
}
