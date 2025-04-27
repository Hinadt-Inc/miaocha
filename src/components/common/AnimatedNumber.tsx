import { useState, useEffect } from 'react';
import { useSpring, animated } from '@react-spring/web';
import { withPureComponent } from '../../utils/withPureComponent';

interface AnimatedNumberProps {
  value: number;
  duration?: number;
  delay?: number;
  formatter?: (value: number) => string;
  className?: string;
  style?: React.CSSProperties;
}

const AnimatedNumber: React.FC<AnimatedNumberProps> = ({
  value,
  duration = 1000,
  delay = 0,
  formatter = (val) => val.toFixed(0),
  className = '',
  style = {}
}) => {
  const [prevValue, setPrevValue] = useState(0);
  
  useEffect(() => {
    // 存储前一个值，用于动画起点
    setPrevValue(value);
  }, [value]);
  
  const { number } = useSpring({
    from: { number: prevValue },
    to: { number: value },
    delay,
    config: { duration },
  });
  
  return (
    <animated.span className={className} style={style}>
      {number.to(formatter)}
    </animated.span>
  );
};

export default withPureComponent(AnimatedNumber);