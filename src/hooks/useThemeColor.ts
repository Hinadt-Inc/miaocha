import { useEffect } from 'react';
import { colorPrimary } from '@/utils/utils';

const useThemeColor = () => {
  useEffect(() => {
    document.documentElement.style.setProperty('--primary-color', colorPrimary);
  }, []);

  return colorPrimary;
};

export default useThemeColor;
