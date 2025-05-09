import React, { useState, useEffect } from 'react';
import { useSpringValue } from '@react-spring/web';
import { withPureComponent } from '@/utils/withPureComponent';

interface AnimatedNumberProps {
  value: number;
  duration?: number;
  formatter?: (value: number) => string;
  className?: string;
  style?: React.CSSProperties;
}

const AnimatedNumber: React.FC<AnimatedNumberProps> = ({
  value,
  duration = 1000,
  formatter = (val) => val.toFixed(0),
  className = '',
  style = {},
}) => {
  const number = useSpringValue(0, { config: { duration } });
  const [displayValue, setDisplayValue] = useState(formatter(0));

  useEffect(() => {
    number.start(value);
    const raf = requestAnimationFrame(() => {
      setDisplayValue(formatter(number.get()));
    });
    return () => cancelAnimationFrame(raf);
  }, [value, number, formatter]);

  return (
    <span className={className} style={style}>
      {displayValue}
    </span>
  );
};

export default withPureComponent(AnimatedNumber);
