/**
 * @providesModule RNShake
 * @flow
 */
'use strict';

import React, { DeviceEventEmitter, NativeModules } from 'react-native';
import invariant from 'invariant';

const { RNShakeEvent } = NativeModules;

var listener;
class RNShake {
  static config(type: int, handler: int) {
    RNShakeEvent.config(type, value);
  }
  static addEventListener(type: string, handler: Function) {
    invariant(
      type === 'ShakeEvent',
      'RNShake only supports `ShakeEvent` event'
    );
    listener = DeviceEventEmitter.addListener('ShakeEvent', () => {
      if (handler) {
        handler();
      }
    });
  }
  static removeEventListener(type: string, handler: Function) {
    invariant(
      type === 'ShakeEvent',
      'RNShake only supports `ShakeEvent` event'
    );
    if (!listener) {
      return;
    }
    if (handler) {
      handler();
    }
    listener.remove();
  }
};

module.exports = RNShake;
