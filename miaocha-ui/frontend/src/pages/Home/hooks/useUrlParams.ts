import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { QUICK_RANGES, DATE_FORMAT_THOUSOND, formatTimeString } from '../utils';
import { STORAGE_KEYS, URL_PARAMS, URL_PARAMS_TO_CLEAN } from '../constants';
import type { ISharedParams } from '../types';

/**
 * URL参数处理相关的hook
 */
export const useUrlParams = (
  sharedParams: ISharedParams | null,
  setSharedParams: (params: ISharedParams | null) => void,
  setSelectedModule: (module: string) => void,
  processedUrlRef: React.MutableRefObject<string>,
) => {
  const [urlSearchParams] = useSearchParams();

  // 页面初始化时检查是否有保存的分享参数
  useEffect(() => {
    const savedSharedParams = sessionStorage.getItem(STORAGE_KEYS.SHARED_PARAMS);
    if (savedSharedParams && !sharedParams) {
      try {
        const parsedSavedParams = JSON.parse(savedSharedParams);

        // 处理保存的时间范围参数
        if (parsedSavedParams.timeRange) {
          if (QUICK_RANGES[parsedSavedParams.timeRange]) {
            // 对于保存的相对时间范围，重新计算时间
            const quickRange = QUICK_RANGES[parsedSavedParams.timeRange];
            parsedSavedParams.startTime = quickRange.from().format(DATE_FORMAT_THOUSOND);
            parsedSavedParams.endTime = quickRange.to().format(DATE_FORMAT_THOUSOND);
          } else if (parsedSavedParams.timeRange.includes(' ~ ')) {
            // 绝对时间范围格式：startTime ~ endTime
            const timeParts = parsedSavedParams.timeRange.split(' ~ ');
            if (timeParts.length === 2) {
              parsedSavedParams.startTime = formatTimeString(timeParts[0]);
              parsedSavedParams.endTime = formatTimeString(timeParts[1]);
            }
          }
        }

        setSharedParams(parsedSavedParams);

        if (parsedSavedParams.module) {
          setSelectedModule(parsedSavedParams.module);
        }
      } catch (e) {
        console.error('解析保存的分享参数失败:', e);
        sessionStorage.removeItem(STORAGE_KEYS.SHARED_PARAMS);
      }
    }
  }, []); // 只在组件挂载时执行一次

  // 处理分享的URL参数
  useEffect(() => {
    const handleSharedParams = () => {
      try {
        const keywords = urlSearchParams.get(URL_PARAMS.KEYWORDS);
        const whereSqls = urlSearchParams.get(URL_PARAMS.WHERE_SQLS);
        const timeRange = urlSearchParams.get(URL_PARAMS.TIME_RANGE);
        const startTime = urlSearchParams.get(URL_PARAMS.START_TIME);
        const endTime = urlSearchParams.get(URL_PARAMS.END_TIME);
        const module = urlSearchParams.get(URL_PARAMS.MODULE);
        const timeGrouping = urlSearchParams.get(URL_PARAMS.TIME_GROUPING);
        const fields = urlSearchParams.get('fields');
        const timeType = urlSearchParams.get('timeType');
        const relativeStartOption = urlSearchParams.get('relativeStartOption');
        const relativeEndOption = urlSearchParams.get('relativeEndOption');

        // 生成当前URL参数的唯一标识
        const currentUrlParams = `${keywords || ''}-${whereSqls || ''}-${timeRange || ''}-${startTime || ''}-${endTime || ''}-${module || ''}-${timeGrouping || ''}-${fields || ''}-${timeType || ''}-${relativeStartOption || ''}-${relativeEndOption || ''}`;

        // 如果已经处理过相同的URL参数，则跳过
        if (processedUrlRef.current === currentUrlParams) {
          return;
        }

        // 如果有分享参数，解析并保存
        if (
          keywords ||
          whereSqls ||
          timeRange ||
          startTime ||
          endTime ||
          module ||
          fields ||
          (timeType === 'relative' && relativeStartOption && relativeEndOption)
        ) {
          const parsedParams: any = {};

          if (keywords) {
            try {
              parsedParams.keywords = JSON.parse(keywords);
            } catch (e) {
              console.error('解析keywords参数失败:', e);
            }
          }

          if (whereSqls) {
            try {
              parsedParams.whereSqls = JSON.parse(whereSqls);
            } catch (e) {
              console.error('解析whereSqls参数失败:', e);
            }
          }

          if (fields) {
            try {
              parsedParams.fields = JSON.parse(fields);
            } catch (e) {
              console.error('解析fields参数失败:', e);
            }
          }

          if (timeRange) parsedParams.timeRange = timeRange;

          // 处理自定义相对时间参数
          if (timeType === 'relative' && relativeStartOption && relativeEndOption) {
            try {
              parsedParams.timeType = 'relative';
              parsedParams.relativeStartOption = JSON.parse(relativeStartOption);
              parsedParams.relativeEndOption = JSON.parse(relativeEndOption);
            } catch (e) {
              console.error('解析相对时间参数失败:', e);
            }
          }

          if (startTime) {
            const formattedStartTime = formatTimeString(startTime);
            parsedParams.startTime = formattedStartTime;
          }
          if (endTime) {
            const formattedEndTime = formatTimeString(endTime);
            parsedParams.endTime = formattedEndTime;
          }
          if (timeGrouping) parsedParams.timeGrouping = timeGrouping;
          if (module) parsedParams.module = module;

          // 保存分享参数到状态和sessionStorage，确保登录后不丢失
          if (Object.keys(parsedParams).length > 0) {
            // 处理时间范围参数
            if (parsedParams.timeRange) {
              if (QUICK_RANGES[parsedParams.timeRange]) {
                // 相对时间范围，重新计算当前时间
                const quickRange = QUICK_RANGES[parsedParams.timeRange];
                parsedParams.startTime = quickRange.from().format(DATE_FORMAT_THOUSOND);
                parsedParams.endTime = quickRange.to().format(DATE_FORMAT_THOUSOND);
              } else if (parsedParams.timeRange.includes(' ~ ')) {
                // 绝对时间范围格式：startTime ~ endTime
                const timeParts = parsedParams.timeRange.split(' ~ ');
                if (timeParts.length === 2) {
                  parsedParams.startTime = formatTimeString(timeParts[0]);
                  parsedParams.endTime = formatTimeString(timeParts[1]);
                }
              }
            }

            // 标记已处理这组URL参数
            processedUrlRef.current = currentUrlParams;

            setSharedParams(parsedParams);

            // 保存到sessionStorage，防止登录跳转后丢失
            sessionStorage.setItem(STORAGE_KEYS.SHARED_PARAMS, JSON.stringify(parsedParams));

            if (parsedParams.module) {
              setSelectedModule(parsedParams.module);
            }
          }
        } else {
          // 如果URL中没有分享参数，检查sessionStorage中是否有保存的分享参数
          const savedSharedParams = sessionStorage.getItem(STORAGE_KEYS.SHARED_PARAMS);
          if (savedSharedParams && !sharedParams) {
            try {
              const parsedSavedParams = JSON.parse(savedSharedParams);
              setSharedParams(parsedSavedParams);

              if (parsedSavedParams.module) {
                setSelectedModule(parsedSavedParams.module);
              }
            } catch (e) {
              console.error('解析保存的分享参数失败:', e);
              sessionStorage.removeItem(STORAGE_KEYS.SHARED_PARAMS);
            }
          }
        }
      } catch (error) {
        console.error('处理分享参数失败:', error);
      }
    };

    handleSharedParams();
  }, [urlSearchParams]); // 移除sharedParams依赖，避免循环

  // 清理URL参数的工具函数
  const cleanupUrlParams = () => {
    const newUrl = new URL(window.location.href);
    const paramsToClean = [...URL_PARAMS_TO_CLEAN, 'fields'];
    paramsToClean.forEach((param) => {
      newUrl.searchParams.delete(param);
    });
    window.history.replaceState({}, '', newUrl.toString());

    // 清理sessionStorage中的分享参数
    sessionStorage.removeItem(STORAGE_KEYS.SHARED_PARAMS);
  };

  return {
    cleanupUrlParams,
  };
};
