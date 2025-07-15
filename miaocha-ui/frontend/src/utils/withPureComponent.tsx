import { memo } from 'react';
export function withPureComponent<P extends object>(
  Component: React.ComponentType<P>,
  compareProps?: (prevProps: P, nextProps: P) => boolean,
) {
  return memo(Component, compareProps);
}
